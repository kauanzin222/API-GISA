package com.fatec.gisa.services.cadastro;

import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.fatec.gisa.dtos.assistentesocial.request.AssistenteSocialCadastroRequestDTO;
import com.fatec.gisa.dtos.assistentesocial.request.AssistenteSocialComUsuarioRequestDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialistaCadastroRequestDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialistaComUsuarioRequestDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialistaPJComUsuarioRequestDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialistaPJCadastroRequestDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalComUsuarioRequestDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalCadastroRequestDTO;
import com.fatec.gisa.dtos.usuario.request.UsuarioRequestDTO;
import com.fatec.gisa.dtos.usuario.response.AcessoConcedidoDTO;
import com.fatec.gisa.dtos.usuario.response.UsuarioResumoDTO;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.assistentesocial.AssistenteSocial;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.especialista.EspecialistaPJ;
import com.fatec.gisa.entities.profissional.Profissional;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.dtos.paciente.request.PacienteCadastroRequestDTO;
import com.fatec.gisa.exceptions.EnvioEmailException;
import com.fatec.gisa.services.assistentesocial.AssistenteSocialService;
import com.fatec.gisa.services.email.EmailService;
import com.fatec.gisa.services.especialista.EspecialistaPJService;
import com.fatec.gisa.services.especialista.EspecialistaService;
import com.fatec.gisa.services.paciente.PacienteService;
import com.fatec.gisa.services.profissional.ProfissionalService;
import com.fatec.gisa.services.profissional.ProfissionalMapper;
import com.fatec.gisa.services.usuario.AcessoCriado;
import com.fatec.gisa.services.usuario.SenhaTemporariaGerada;
import com.fatec.gisa.services.usuario.UsuarioService;

import lombok.RequiredArgsConstructor;

// Os fluxos que criam acesso gravam o cadastro e o usuário numa transação e só depois enviam o
// e-mail com a senha temporária: se o envio falhar, o cadastro não é desfeito (emailEnviado: false)
@Service
@RequiredArgsConstructor
public class CadastroFacadeService {

    private final EspecialistaService especialistaService;
    private final EspecialistaPJService especialistaPJService;
    private final AssistenteSocialService assistenteSocialService;
    private final UsuarioService usuarioService;
    private final ProfissionalService profissionalService;
    private final ProfissionalMapper profissionalMapper;
    private final PacienteService pacienteService;
    private final EmailService emailService;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public Paciente cadastrarPaciente(PacienteCadastroRequestDTO dto) {
        return pacienteService.cadastrar(dto);
    }

    @Transactional
    public Especialista cadastrarEspecialista(EspecialistaCadastroRequestDTO dto) {
        return especialistaService.cadastrar(dto);
    }

    @Transactional
    public EspecialistaPJ cadastrarEspecialistaPJ(EspecialistaPJCadastroRequestDTO dto) {
        return especialistaPJService.cadastrar(dto);
    }

    @Transactional
    public Profissional cadastrarProfissional(ProfissionalCadastroRequestDTO dto) {
        return profissionalService.criarProfissional(dto);
    }

    @Transactional
    public AssistenteSocial cadastrarAssistenteSocial(AssistenteSocialCadastroRequestDTO dto) {
        return assistenteSocialService.cadastrar(dto);
    }

    public CadastroComAcesso<Profissional> cadastrarProfissionalComUsuario(ProfissionalComUsuarioRequestDTO request) {
        return comAcesso(() -> profissionalService.criarProfissional(request.profissional()), request.usuario());
    }

    public CadastroComAcesso<Especialista> cadastrarEspecialistaComUsuario(EspecialistaComUsuarioRequestDTO request) {
        return comAcesso(() -> especialistaService.cadastrar(request.especialista()), request.usuario());
    }

    public CadastroComAcesso<EspecialistaPJ> cadastrarEspecialistaPJComUsuario(
            EspecialistaPJComUsuarioRequestDTO request) {
        return comAcesso(() -> especialistaPJService.cadastrar(request.especialistaPJ()), request.usuario());
    }

    public CadastroComAcesso<AssistenteSocial> cadastrarAssistenteSocialComUsuario(
            AssistenteSocialComUsuarioRequestDTO request) {
        return comAcesso(() -> assistenteSocialService.cadastrar(request.assistenteSocial()), request.usuario());
    }

    public AcessoConcedidoDTO concederAcessoProfissional(Long idCadastro, UsuarioRequestDTO dto) {
        return concederAcesso(() -> profissionalService.buscarParaConcederAcesso(idCadastro), dto);
    }

    public AcessoConcedidoDTO concederAcessoEspecialista(Long idCadastro, UsuarioRequestDTO dto) {
        return concederAcesso(() -> especialistaService.buscarParaConcederAcesso(idCadastro), dto);
    }

    public AcessoConcedidoDTO concederAcessoEspecialistaPJ(Long idCadastro, UsuarioRequestDTO dto) {
        return concederAcesso(() -> especialistaPJService.buscarParaConcederAcesso(idCadastro), dto);
    }

    public AcessoConcedidoDTO concederAcessoAssistenteSocial(Long idCadastro, UsuarioRequestDTO dto) {
        return concederAcesso(() -> assistenteSocialService.buscarParaConcederAcesso(idCadastro), dto);
    }

    // POST /api/usuarios/{idCadastro}/senha-temporaria. Aqui o e-mail é a única forma de a pessoa
    // receber a senha: se o envio falhar, nada é gravado (a senha anterior continua valendo) e a
    // resposta é 503, para o administrador tentar de novo
    public void gerarNovaSenhaTemporaria(Long idCadastro) {
        transactionTemplate.executeWithoutResult(status -> {
            SenhaTemporariaGerada senha = usuarioService.gerarNovaSenhaTemporaria(idCadastro);
            if (!enviar(senha)) {
                throw new EnvioEmailException("Não foi possível enviar o e-mail com a nova senha temporária. "
                        + "Nada foi alterado; tente novamente mais tarde.");
            }
        });
    }

    private <T extends Pessoa> CadastroComAcesso<T> comAcesso(Supplier<T> cadastrar, UsuarioRequestDTO usuarioDto) {
        CadastroPendente<T> pendente = transactionTemplate.execute(status -> {
            T cadastro = cadastrar.get();
            AcessoCriado acesso = usuarioService.criarUsuarioParaPessoa(cadastro, usuarioDto);
            return new CadastroPendente<>(cadastro, acesso.senhaTemporaria());
        });
        return new CadastroComAcesso<>(pendente.cadastro(), enviar(pendente.senha()));
    }

    private AcessoConcedidoDTO concederAcesso(Supplier<? extends Pessoa> buscar, UsuarioRequestDTO dto) {
        AcessoPendente pendente = transactionTemplate.execute(status -> {
            AcessoCriado acesso = usuarioService.criarAcessoParaPessoa(buscar.get(), dto);
            return new AcessoPendente(profissionalMapper.toUsuarioResumoDTO(acesso.usuario()), acesso.senhaTemporaria());
        });
        return AcessoConcedidoDTO.de(pendente.usuario(), enviar(pendente.senha()));
    }

    private boolean enviar(SenhaTemporariaGerada senha) {
        return emailService.enviarSenhaTemporaria(senha.nome(), senha.email(), senha.senha(), senha.expiraEm());
    }

    private record CadastroPendente<T>(T cadastro, SenhaTemporariaGerada senha) {
    }

    private record AcessoPendente(UsuarioResumoDTO usuario, SenhaTemporariaGerada senha) {
    }
}
