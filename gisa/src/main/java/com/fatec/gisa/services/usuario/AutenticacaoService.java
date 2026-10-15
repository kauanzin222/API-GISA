package com.fatec.gisa.services.usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.core.task.TaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.fatec.gisa.dtos.usuario.request.RedefinirSenhaRequestDTO;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.usuario.TokenRedefinicaoSenha;
import com.fatec.gisa.entities.usuario.Usuario;
import com.fatec.gisa.enums.OrigemTokenRedefinicao;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.exceptions.NaoAutorizadoException;
import com.fatec.gisa.repositories.usuario.TokenRedefinicaoSenhaRepository;
import com.fatec.gisa.repositories.usuario.UsuarioRepository;
import com.fatec.gisa.services.email.EmailService;

import lombok.RequiredArgsConstructor;

// Login por e-mail, primeiro acesso com senha temporária, "esqueci minha senha" e redefinição.
// O token JWT continua identificando o usuário pelo CPF (ver JwtService).
@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    private static final Duration VALIDADE_TOKEN_REDEFINICAO = Duration.ofMinutes(60);
    private static final int BYTES_TOKEN = 32;

    private final UsuarioRepository usuarioRepository;
    private final TokenRedefinicaoSenhaRepository tokenRedefinicaoSenhaRepository;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TransactionTemplate transactionTemplate;
    private final TaskExecutor taskExecutor;
    private final SecureRandom aleatorio = new SecureRandom();

    // Usuário dono do e-mail (ativo ou não; o cadastro inativo é recusado na autenticação)
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        List<Usuario> usuarios = usuarioRepository.buscarPorEmail(email.trim());
        return usuarios.size() == 1 ? Optional.of(usuarios.get(0)) : Optional.empty();
    }

    // Chamado depois de a senha ser conferida. Senha temporária expirada: 401 com mensagem própria.
    // Válida: devolve um token de redefinição (PRIMEIRO_ACESSO) no lugar do token de login
    @Transactional
    public Optional<String> verificarSenhaTemporaria(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new NaoAutorizadoException("Credenciais inválidas."));
        if (!usuario.isSenhaTemporaria()) {
            return Optional.empty();
        }
        if (usuario.getSenhaTemporariaExpiraEm() == null || usuario.getSenhaTemporariaExpiraEm().isBefore(Instant.now())) {
            throw new NaoAutorizadoException("Senha temporária expirada. Use 'Esqueci minha senha' "
                    + "ou peça um novo acesso ao administrador.");
        }
        return Optional.of(gerarToken(usuario.getIdCadastro(), OrigemTokenRedefinicao.PRIMEIRO_ACESSO));
    }

    // Sempre termina sem erro, exista o e-mail ou não. O envio roda em segundo plano, para que o
    // tempo de resposta também não revele se o e-mail está cadastrado
    public void esqueciSenha(String email) {
        Optional<Usuario> encontrado = buscarPorEmail(email);
        if (encontrado.isEmpty()) {
            return;
        }
        Long idUsuario = encontrado.get().getIdCadastro();
        EnvioLink envio = transactionTemplate.execute(status -> {
            Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
            if (usuario == null || usuario.getPessoa().getStatusCadastro() != StatusCadastro.ATIVO) {
                return null;
            }
            Pessoa pessoa = usuario.getPessoa();
            String token = gerarToken(idUsuario, OrigemTokenRedefinicao.ESQUECI_SENHA);
            return new EnvioLink(pessoa.getNome(), pessoa.getEmail(), token);
        });
        if (envio != null) {
            taskExecutor.execute(() -> emailService.enviarLinkRedefinicao(
                    envio.nome(), envio.email(), envio.token(), VALIDADE_TOKEN_REDEFINICAO));
        }
    }

    private record EnvioLink(String nome, String email, String token) {
    }

    // Primeiro acesso e "esqueci minha senha". Devolve o CPF do usuário, para o controller emitir
    // o token de login (a senha nova já vale)
    @Transactional
    public String redefinirSenha(RedefinirSenhaRequestDTO dto) {
        TokenRedefinicaoSenha token = tokenRedefinicaoSenhaRepository.findByTokenHash(hash(dto.token()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Link de redefinição inválido. Solicite um novo em 'Esqueci minha senha'."));
        if (token.getUsadoEm() != null) {
            throw new IllegalArgumentException(
                    "Este link de redefinição já foi usado ou foi substituído por um mais novo. "
                            + "Solicite um novo em 'Esqueci minha senha'.");
        }
        if (token.getExpiraEm().isBefore(Instant.now())) {
            throw new IllegalArgumentException(
                    "Link de redefinição expirado. Solicite um novo em 'Esqueci minha senha'.");
        }
        Usuario usuario = usuarioRepository.findById(token.getIdUsuario())
                .filter(encontrado -> encontrado.getPessoa().getStatusCadastro() == StatusCadastro.ATIVO)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Link de redefinição inválido. Solicite um novo em 'Esqueci minha senha'."));

        if (!dto.novaSenha().equals(dto.confirmacaoNovaSenha())) {
            throw new IllegalArgumentException("A confirmação não corresponde à nova senha.");
        }
        if (usuario.isSenhaTemporaria() && passwordEncoder.matches(dto.novaSenha(), usuario.getSenha())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha temporária.");
        }

        Instant agora = Instant.now();
        token.setUsadoEm(agora);
        tokenRedefinicaoSenhaRepository.save(token);
        tokenRedefinicaoSenhaRepository.invalidarAbertos(usuario.getIdCadastro(), agora);
        usuarioService.trocarSenha(usuario, dto.novaSenha());
        return usuario.getPessoa().getCpf();
    }

    // Um token novo invalida os anteriores do mesmo usuário. O token vai para o usuário; o banco
    // guarda só o hash
    private String gerarToken(Long idUsuario, OrigemTokenRedefinicao origem) {
        Instant agora = Instant.now();
        tokenRedefinicaoSenhaRepository.invalidarAbertos(idUsuario, agora);

        byte[] bytes = new byte[BYTES_TOKEN];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        tokenRedefinicaoSenhaRepository.save(new TokenRedefinicaoSenha(
                null, idUsuario, hash(token), origem, agora.plus(VALIDADE_TOKEN_REDEFINICAO), null, agora));
        return token;
    }

    private static String hash(String token) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException("SHA-256 indisponível", erro);
        }
    }
}
