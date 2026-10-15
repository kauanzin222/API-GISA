package com.fatec.gisa.controllers.usuario;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
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
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.especialista.EspecialistaPJ;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Cbo;
import com.fatec.gisa.entities.profissional.Profissional;
import com.fatec.gisa.entities.usuario.Perfil;
import com.fatec.gisa.entities.usuario.Permissao;
import com.fatec.gisa.entities.usuario.Usuario;
import com.fatec.gisa.enums.EstadoCivil;
import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManager;

// GET /api/usuarios/me: o usuário é identificado pelo token, e o tipo vem da classe real do cadastro
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:usuario-logado;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef"
})
@AutoConfigureMockMvc
class UsuarioLogadoTest {

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

    private Perfil perfil;
    private Cargo cargo;

    @BeforeEach
    void preparar() {
        transactionTemplate.executeWithoutResult(status -> {
            Permissao segunda = new Permissao(null, "PERMISSAO_B" + SEQUENCIA.incrementAndGet(), "Segunda", null, null);
            Permissao primeira = new Permissao(null, "PERMISSAO_A" + SEQUENCIA.incrementAndGet(), "Primeira", null, null);
            entityManager.persist(primeira);
            entityManager.persist(segunda);
            perfil = new Perfil(null, "Especialista" + SEQUENCIA.incrementAndGet(),
                    new ArrayList<>(List.of(segunda, primeira)));
            entityManager.persist(perfil);

            Cbo cbo = new Cbo("9" + SEQUENCIA.incrementAndGet(), "Teste");
            entityManager.persist(cbo);
            cargo = new Cargo(null, "Terapeuta", cbo);
            entityManager.persist(cargo);
        });
    }

    @Test
    void retornaOsDadosDoEspecialistaLogado() throws Exception {
        Especialista especialista = criarComUsuario(new Especialista(), "Mariana Costa");

        me(login(especialista.getCpf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCadastro").value(especialista.getIdCadastro()))
                .andExpect(jsonPath("$.nome").value("Mariana Costa"))
                .andExpect(jsonPath("$.cpf").value(especialista.getCpf()))
                .andExpect(jsonPath("$.tipoCadastro").value("ESPECIALISTA"))
                .andExpect(jsonPath("$.perfil.idPerfil").value(perfil.getIdPerfil()))
                .andExpect(jsonPath("$.perfil.nome").value(perfil.getNome()))
                .andExpect(jsonPath("$.permissoes.length()").value(2))
                // Ordenadas pelo ID
                .andExpect(jsonPath("$.permissoes[0].descricao").value("Primeira"))
                .andExpect(jsonPath("$.permissoes[1].descricao").value("Segunda"))
                .andExpect(jsonPath("$.usuario").doesNotExist());
    }

    @Test
    void distingueProfissionalEspecialistaPJEPessoaSemVinculo() throws Exception {
        Profissional profissional = criarComUsuario(new Profissional(), "Paulo Lima");
        EspecialistaPJ pj = criarComUsuario(new EspecialistaPJ(), "Clara PJ");
        Pessoa administrador = criarComUsuario(new Pessoa(), "Administrador do Sistema");

        me(login(profissional.getCpf())).andExpect(jsonPath("$.tipoCadastro").value("PROFISSIONAL"));
        me(login(pj.getCpf())).andExpect(jsonPath("$.tipoCadastro").value("ESPECIALISTA_PJ"));
        me(login(administrador.getCpf())).andExpect(jsonPath("$.tipoCadastro").value("PESSOA"));
    }

    @Test
    void semTokenNaoResponde() throws Exception {
        int status = mockMvc.perform(get("/api/usuarios/me")).andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
    }

    private ResultActions me(String token) throws Exception {
        return mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + token));
    }

    private String login(String cpf) throws Exception {
        String resposta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"cpf\": \"%s\", \"senha\": \"%s\" }".formatted(cpf, SENHA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.token");
    }

    private <T extends Pessoa> T criarComUsuario(T pessoa, String nome) {
        return transactionTemplate.execute(status -> {
            String cpf = "%011d".formatted(60000000000L + SEQUENCIA.incrementAndGet());
            pessoa.setNome(nome);
            pessoa.setCpf(cpf);
            pessoa.setDataNascimento(LocalDate.of(1990, 1, 1));
            pessoa.setSexo('F');
            pessoa.setEstadoCivil(EstadoCivil.SOLTEIRO);
            if (pessoa instanceof Profissional profissional) {
                profissional.setEmail(cpf + "@email.com");
                profissional.setCargo(cargo);
            }
            if (pessoa instanceof Especialista especialista) {
                especialista.setRegistroConselho("CRP-" + cpf);
            }
            if (pessoa instanceof EspecialistaPJ pj) {
                pj.setCnpj("000" + cpf);
                pj.setRazaoSocial("Clínica " + nome);
            }
            entityManager.persist(pessoa);

            Usuario usuario = new Usuario();
            usuario.setPessoa(pessoa);
            usuario.setSenha(passwordEncoder.encode(SENHA));
            usuario.setPerfil(perfil);
            entityManager.persist(usuario);
            return pessoa;
        });
    }
}
