package com.fatec.gisa.controllers.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.usuario.Perfil;
import com.fatec.gisa.entities.usuario.Permissao;
import com.fatec.gisa.entities.usuario.Usuario;
import com.fatec.gisa.enums.EstadoCivil;
import com.fatec.gisa.repositories.usuario.PermissaoRepository;
import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManager;

// Testa pela camada HTTP: filtro JWT, @PreAuthorize e formato das respostas de erro
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:usuario-senha;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef"
})
@AutoConfigureMockMvc
class UsuarioSenhaTest {

    private static final AtomicInteger SEQUENCIA = new AtomicInteger();
    private static final String SENHA = "SenhaInicial1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PermissaoRepository permissaoRepository;

    private String cpfAdmin;
    private String cpfComum;
    private Long idComum;

    // Dados novos a cada teste (CPFs distintos), pois as transações são confirmadas
    @BeforeEach
    void preparar() {
        transactionTemplate.executeWithoutResult(status -> {
            Permissao acessoTotal = permissaoRepository.findByNome("ACESSO_TOTAL").orElseGet(() -> {
                Permissao nova = new Permissao(null, "ACESSO_TOTAL", null, null);
                entityManager.persist(nova);
                return nova;
            });
            Perfil admin = perfil("Admin", List.of(acessoTotal));
            Perfil comum = perfil("Comum", List.of());

            cpfAdmin = cpfNovo();
            usuario(cpfAdmin, admin);
            cpfComum = cpfNovo();
            idComum = usuario(cpfComum, comum).getIdCadastro();
        });
    }

    @Test
    void alteraAPropriaSenhaEInvalidaOTokenAnterior() throws Exception {
        String tokenAntigo = login(cpfComum, SENHA);

        String resposta = patchJson("/api/usuarios/minha-senha", tokenAntigo, """
                { "senhaAtual": "%s", "novaSenha": "NovaSenha123", "confirmacaoNovaSenha": "NovaSenha123" }
                """.formatted(SENHA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String tokenNovo = JsonPath.read(resposta, "$.token");

        assertThat(rotaProtegida(tokenAntigo)).isIn(401, 403);
        assertThat(rotaProtegida(tokenNovo)).isEqualTo(200);
        tentarLogin(cpfComum, SENHA).andExpect(status().isUnauthorized());
        tentarLogin(cpfComum, "NovaSenha123").andExpect(status().isOk());
    }

    @Test
    void recusaSenhaAtualIncorreta() throws Exception {
        patchJson("/api/usuarios/minha-senha", login(cpfComum, SENHA), """
                { "senhaAtual": "errada123", "novaSenha": "NovaSenha123", "confirmacaoNovaSenha": "NovaSenha123" }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A senha atual está incorreta."));
    }

    @Test
    void recusaConfirmacaoDiferente() throws Exception {
        patchJson("/api/usuarios/minha-senha", login(cpfComum, SENHA), """
                { "senhaAtual": "%s", "novaSenha": "NovaSenha123", "confirmacaoNovaSenha": "Outra12345" }
                """.formatted(SENHA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A confirmação não corresponde à nova senha."));
    }

    @Test
    void recusaNovaSenhaIgualAAtual() throws Exception {
        patchJson("/api/usuarios/minha-senha", login(cpfComum, SENHA), """
                { "senhaAtual": "%s", "novaSenha": "%s", "confirmacaoNovaSenha": "%s" }
                """.formatted(SENHA, SENHA, SENHA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A nova senha deve ser diferente da senha atual."));
    }

    @Test
    void recusaNovaSenhaCurta() throws Exception {
        patchJson("/api/usuarios/minha-senha", login(cpfComum, SENHA), """
                { "senhaAtual": "%s", "novaSenha": "curta", "confirmacaoNovaSenha": "curta" }
                """.formatted(SENHA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.novaSenha").value("A senha deve conter entre 8 e 30 caracteres."));
    }

    @Test
    void adminRedefineASenhaEInvalidaOsTokensDoUsuario() throws Exception {
        String tokenDoUsuario = login(cpfComum, SENHA);

        patchJson("/api/usuarios/" + idComum + "/senha", login(cpfAdmin, SENHA), """
                { "novaSenha": "Redefinida123" }
                """)
                .andExpect(status().isNoContent());

        assertThat(rotaProtegida(tokenDoUsuario)).isIn(401, 403);
        tentarLogin(cpfComum, "Redefinida123").andExpect(status().isOk());
    }

    @Test
    void usuarioSemPermissaoNaoRedefineSenhaDeOutro() throws Exception {
        patchJson("/api/usuarios/" + idComum + "/senha", login(cpfComum, SENHA), """
                { "novaSenha": "Redefinida123" }
                """)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Você não tem permissão para realizar esta ação."));

        tentarLogin(cpfComum, SENHA).andExpect(status().isOk());
    }

    @Test
    void redefinirCadastroSemUsuarioRetorna404() throws Exception {
        Long idSemUsuario = transactionTemplate.execute(status -> pessoa(cpfNovo()).getIdCadastro());

        patchJson("/api/usuarios/" + idSemUsuario + "/senha", login(cpfAdmin, SENHA), """
                { "novaSenha": "Redefinida123" }
                """)
                .andExpect(status().isNotFound());
    }

    private String login(String cpf, String senha) throws Exception {
        String resposta = tentarLogin(cpf, senha)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.token");
    }

    private ResultActions tentarLogin(String cpf, String senha) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"cpf\": \"%s\", \"senha\": \"%s\" }".formatted(cpf, senha)));
    }

    private ResultActions patchJson(String url, String token, String corpo) throws Exception {
        return mockMvc.perform(patch(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private int rotaProtegida(String token) throws Exception {
        return mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus();
    }

    private Perfil perfil(String nome, List<Permissao> permissoes) {
        Perfil perfil = new Perfil(null, nome + SEQUENCIA.incrementAndGet(), new java.util.ArrayList<>(permissoes));
        entityManager.persist(perfil);
        return perfil;
    }

    private Usuario usuario(String cpf, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setPessoa(pessoa(cpf));
        usuario.setSenha(passwordEncoder.encode(SENHA));
        usuario.setPerfil(perfil);
        entityManager.persist(usuario);
        return usuario;
    }

    private Pessoa pessoa(String cpf) {
        Pessoa pessoa = new Pessoa();
        pessoa.setNome("Pessoa " + cpf);
        pessoa.setCpf(cpf);
        pessoa.setDataNascimento(LocalDate.of(1990, 1, 1));
        pessoa.setSexo('F');
        pessoa.setEstadoCivil(EstadoCivil.SOLTEIRO);
        entityManager.persist(pessoa);
        return pessoa;
    }

    private String cpfNovo() {
        return "%011d".formatted(50000000000L + SEQUENCIA.incrementAndGet());
    }
}
