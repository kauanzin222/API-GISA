package com.fatec.gisa.controllers.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Cbo;
import com.fatec.gisa.entities.profissional.Profissional;
import com.fatec.gisa.entities.usuario.Perfil;
import com.fatec.gisa.entities.usuario.Permissao;
import com.fatec.gisa.entities.usuario.Usuario;
import com.fatec.gisa.enums.EstadoCivil;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.repositories.usuario.PermissaoRepository;
import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManager;

// Administração de acessos (/api/usuarios) pela camada HTTP: permissões, travas de segurança
// e invalidação dos tokens ao retirar o acesso
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:acesso-usuario;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef"
})
@AutoConfigureMockMvc
class AcessoUsuarioTest {

    private static final AtomicInteger SEQUENCIA = new AtomicInteger();
    private static final String SENHA = "SenhaInicial1";
    private static final String SEM_PERMISSAO = "Você não tem permissão para realizar esta ação.";
    private static final String ULTIMO_ADMINISTRADOR = "O sistema precisa de ao menos um usuário ativo com acesso total.";
    private static final String ULTIMO_GESTOR_DE_PERFIS = "Operação bloqueada: o sistema precisa de ao menos um usuário "
            + "ativo com permissão para gerenciar perfis (GERENCIAR_PERFIS ou ACESSO_TOTAL).";

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

    private Perfil perfilAdministrador;
    private Perfil perfilGestor;
    private Perfil perfilEditor;
    private Perfil perfilComum;
    private Cargo cargo;

    // Administrador inicial (tipo PESSOA), gestor de acessos (profissional), editor de profissionais
    // e um especialista sem permissões. Os usuários dos testes anteriores são apagados, para que a
    // contagem de administradores ativos comece sempre em 1
    private Pessoa administrador;
    private Profissional gestor;
    private Profissional editor;
    private Especialista comum;

    @BeforeEach
    void preparar() {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createQuery("delete from Usuario").executeUpdate();

            perfilAdministrador = perfil("Administrador", permissao("ACESSO_TOTAL"));
            perfilGestor = perfil("Gestor", permissao("GERENCIAR_USUARIOS"), permissao("EDITAR_PROFISSIONAL"));
            perfilEditor = perfil("Editor", permissao("EDITAR_PROFISSIONAL"));
            perfilComum = perfil("Comum");

            Cbo cbo = new Cbo("8" + SEQUENCIA.incrementAndGet(), "Teste");
            entityManager.persist(cbo);
            cargo = new Cargo(null, "Psicóloga " + SEQUENCIA.incrementAndGet(), cbo);
            entityManager.persist(cargo);

            administrador = comUsuario(new Pessoa(), "Administrador do Sistema", perfilAdministrador);
            gestor = comUsuario(new Profissional(), "Gabriela Gestora", perfilGestor);
            editor = comUsuario(new Profissional(), "Eduardo Editor", perfilEditor);
            comum = comUsuario(new Especialista(), "Mariana Costa", perfilComum);
        });
    }

    // ---------- Listagem e detalhes ----------

    @Test
    void listaUsuariosComFiltros() throws Exception {
        String tokenGestor = login(gestor);

        String resposta = chamar(tokenGestor, get("/api/usuarios?tipoCadastro=PESSOA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].cpf").value(administrador.getCpf()))
                .andExpect(jsonPath("$.content[0].tipoCadastro").value("PESSOA"))
                .andExpect(jsonPath("$.content[0].perfil.nome").value(perfilAdministrador.getNome()))
                .andExpect(jsonPath("$.content[0].statusCadastro").value("ATIVO"))
                .andExpect(jsonPath("$.page.size").value(9))
                .andReturn().getResponse().getContentAsString();
        assertThat(resposta).doesNotContainIgnoringCase("senha");

        chamar(tokenGestor, get("/api/usuarios?idPerfil=" + perfilComum.getIdPerfil()))
                .andExpect(jsonPath("$.content[*].nome", contains("Mariana Costa")));
        chamar(tokenGestor, get("/api/usuarios?tipoCadastro=PROFISSIONAL&tipoCadastro=ESPECIALISTA"))
                .andExpect(jsonPath("$.content[*].nome", contains("Eduardo Editor", "Gabriela Gestora", "Mariana Costa")));
        chamar(tokenGestor, get("/api/usuarios?nome=MARIANA&cpf=" + comum.getCpf().substring(0, 3) + "."
                + comum.getCpf().substring(3)))
                .andExpect(jsonPath("$.content.length()").value(1));
        chamar(tokenGestor, get("/api/usuarios?statusCadastro=INATIVO"))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void detalhesDoAcessoDeProfissionalEDoAdministradorInicial() throws Exception {
        String tokenGestor = login(gestor);

        chamar(tokenGestor, get("/api/usuarios/" + gestor.getIdCadastro()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(gestor.getEmail()))
                .andExpect(jsonPath("$.tipoCadastro").value("PROFISSIONAL"))
                .andExpect(jsonPath("$.cargo.idCargo").value(cargo.getIdCargo()))
                .andExpect(jsonPath("$.cargo.nome").value(cargo.getNome()))
                .andExpect(jsonPath("$.permissoes[*].nome", contains("GERENCIAR_USUARIOS", "EDITAR_PROFISSIONAL")))
                .andExpect(jsonPath("$.senha").doesNotExist());

        chamar(tokenGestor, get("/api/usuarios/" + administrador.getIdCadastro()))
                .andExpect(jsonPath("$.tipoCadastro").value("PESSOA"))
                .andExpect(jsonPath("$.email").value(administrador.getEmail()))
                .andExpect(jsonPath("$.cargo").doesNotExist())
                .andExpect(jsonPath("$.permissoes[0].nome").value("ACESSO_TOTAL"));
    }

    @Test
    void cadastroSemUsuarioOuInexistenteResponde404() throws Exception {
        String tokenGestor = login(gestor);
        Long semUsuario = transactionTemplate.execute(status -> pessoa(new Pessoa(), "Sem Acesso").getIdCadastro());

        for (MockHttpServletRequestBuilder requisicao : List.of(
                get("/api/usuarios/" + semUsuario),
                patchJson("/api/usuarios/" + semUsuario + "/perfil", "{ \"idPerfil\": " + perfilComum.getIdPerfil() + " }"),
                delete("/api/usuarios/" + semUsuario))) {
            chamar(tokenGestor, requisicao)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Este cadastro não possui usuário de acesso. ID: " + semUsuario));
        }
        chamar(tokenGestor, get("/api/usuarios/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cadastro não encontrado com ID: 999999"));
    }

    @Test
    void semGerenciarUsuariosRecebe403() throws Exception {
        String tokenComum = login(comum);

        for (MockHttpServletRequestBuilder requisicao : List.of(
                get("/api/usuarios"),
                get("/api/usuarios/" + gestor.getIdCadastro()),
                patchJson("/api/usuarios/" + gestor.getIdCadastro() + "/perfil", "{ \"idPerfil\": 1 }"),
                delete("/api/usuarios/" + gestor.getIdCadastro()))) {
            chamar(tokenComum, requisicao)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(SEM_PERMISSAO));
        }
    }

    // ---------- Trocar perfil ----------

    @Test
    void trocaOPerfilDeQualquerTipoDeCadastro() throws Exception {
        String tokenGestor = login(gestor);

        chamar(tokenGestor, patchJson("/api/usuarios/" + comum.getIdCadastro() + "/perfil",
                "{ \"idPerfil\": " + perfilEditor.getIdPerfil() + " }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil.idPerfil").value(perfilEditor.getIdPerfil()))
                .andExpect(jsonPath("$.permissoes[0].nome").value("EDITAR_PROFISSIONAL"));

        chamar(tokenGestor, patchJson("/api/usuarios/" + comum.getIdCadastro() + "/perfil", "{ \"idPerfil\": 999999 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Perfil não encontrado com ID: 999999"));
        chamar(tokenGestor, patchJson("/api/usuarios/" + comum.getIdCadastro() + "/perfil", "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.idPerfil").value("O ID do perfil é obrigatório."));
    }

    // O PATCH de profissional com idPerfil exige também GERENCIAR_USUARIOS
    @Test
    void patchDeProfissionalComIdPerfilExigeGerenciarUsuarios() throws Exception {
        String rota = "/api/especialistas/" + comum.getIdCadastro();

        chamar(login(editor), patchJson(rota, "{ \"idPerfil\": " + perfilEditor.getIdPerfil() + " }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(SEM_PERMISSAO));
        chamar(login(editor), patchJson(rota, "{ \"nome\": \"Mariana Costa Lima\" }"))
                .andExpect(status().isOk());

        chamar(login(gestor), patchJson(rota, "{ \"idPerfil\": " + perfilEditor.getIdPerfil() + " }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profissional.usuario.perfil.idPerfil").value(perfilEditor.getIdPerfil()));
    }

    // ---------- Travas de segurança ----------

    @Test
    void naoDeixaOSistemaSemAdministradorAtivo() throws Exception {
        String tokenGestor = login(gestor);
        String rotaAdministrador = "/api/usuarios/" + administrador.getIdCadastro();

        // O administrador é também o único que gerencia perfis: essa trava (409) vem antes
        chamar(tokenGestor, patchJson(rotaAdministrador + "/perfil", "{ \"idPerfil\": " + perfilComum.getIdPerfil() + " }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(ULTIMO_GESTOR_DE_PERFIS));
        chamar(tokenGestor, delete(rotaAdministrador))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(ULTIMO_GESTOR_DE_PERFIS));

        // Um segundo administrador inativo não conta
        transactionTemplate.executeWithoutResult(status -> {
            Profissional inativo = comUsuario(new Profissional(), "Administrador Inativo", perfilAdministrador);
            inativo.setStatusCadastro(StatusCadastro.INATIVO);
        });
        chamar(tokenGestor, delete(rotaAdministrador)).andExpect(status().isConflict());

        // Com outro usuário ativo que gerencia perfis (sem acesso total), vale a trava do administrador
        transactionTemplate.executeWithoutResult(status ->
                comUsuario(new Profissional(), "Gestor de Perfis", perfil("Perfis", permissao("GERENCIAR_PERFIS"))));
        chamar(tokenGestor, patchJson(rotaAdministrador + "/perfil", "{ \"idPerfil\": " + perfilComum.getIdPerfil() + " }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ULTIMO_ADMINISTRADOR));
        chamar(tokenGestor, delete(rotaAdministrador))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ULTIMO_ADMINISTRADOR));

        // Com outro administrador ativo, o primeiro pode perder o acesso total
        chamar(tokenGestor, patchJson("/api/usuarios/" + comum.getIdCadastro() + "/perfil",
                "{ \"idPerfil\": " + perfilAdministrador.getIdPerfil() + " }"))
                .andExpect(status().isOk());
        chamar(tokenGestor, patchJson(rotaAdministrador + "/perfil", "{ \"idPerfil\": " + perfilComum.getIdPerfil() + " }"))
                .andExpect(status().isOk());
    }

    @Test
    void naoRetiraOProprioAcesso() throws Exception {
        chamar(login(gestor), delete("/api/usuarios/" + gestor.getIdCadastro()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Você não pode retirar o seu próprio acesso."));
    }

    // ---------- Retirar acesso ----------

    @Test
    void retirarAcessoInvalidaOsTokensNaHoraEOAcessoPodeSerConcedidoDeNovo() throws Exception {
        String tokenGestor = login(gestor);
        String tokenAntigo = login(comum);

        chamar(tokenGestor, delete("/api/usuarios/" + comum.getIdCadastro())).andExpect(status().isNoContent());

        assertThat(statusDe(tokenAntigo)).isEqualTo(401);
        tentarLogin(comum.getCpf()).andExpect(status().isUnauthorized());
        // O cadastro continua ativo; só o usuário foi apagado
        chamar(tokenGestor, get("/api/usuarios/" + comum.getIdCadastro()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Este cadastro não possui usuário de acesso. ID: " + comum.getIdCadastro()));
        StatusCadastro statusDoCadastro = transactionTemplate.execute(status ->
                entityManager.find(Especialista.class, comum.getIdCadastro()).getStatusCadastro());
        assertThat(statusDoCadastro).isEqualTo(StatusCadastro.ATIVO);

        // Concedido de novo: o usuário recomeça a versão do token em 0, mas o token antigo continua recusado.
        // O acesso novo vem com senha temporária (enviada por e-mail), então a senha antiga não vale mais
        chamar(tokenGestor, post("/api/especialistas/" + comum.getIdCadastro() + "/usuario")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"idPerfil\": %d }".formatted(perfilComum.getIdPerfil())))
                .andExpect(status().isCreated());
        assertThat(statusDe(tokenAntigo)).isEqualTo(401);
        tentarLogin(comum.getCpf()).andExpect(status().isUnauthorized());
        chamar(tokenGestor, get("/api/usuarios/" + comum.getIdCadastro())).andExpect(status().isOk());
    }

    // ---------- Apoio ----------

    private int statusDe(String token) throws Exception {
        return chamar(token, get("/api/usuarios/me")).andReturn().getResponse().getStatus();
    }

    private String login(Pessoa pessoa) throws Exception {
        String resposta = tentarLogin(pessoa.getCpf())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.token");
    }

    private ResultActions tentarLogin(String cpf) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"%s@email.com\", \"senha\": \"%s\" }".formatted(cpf, SENHA)));
    }

    private ResultActions chamar(String token, MockHttpServletRequestBuilder requisicao) throws Exception {
        return mockMvc.perform(requisicao.header("Authorization", "Bearer " + token));
    }

    private MockHttpServletRequestBuilder patchJson(String url, String corpo) {
        return patch(url).contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private Permissao permissao(String nome) {
        return permissaoRepository.findByNome(nome).orElseGet(() -> {
            Permissao nova = new Permissao(null, nome, null, null, null);
            entityManager.persist(nova);
            return nova;
        });
    }

    private Perfil perfil(String nome, Permissao... permissoes) {
        Perfil perfil = new Perfil(null, nome + SEQUENCIA.incrementAndGet(), new ArrayList<>(List.of(permissoes)));
        entityManager.persist(perfil);
        return perfil;
    }

    private <T extends Pessoa> T comUsuario(T pessoa, String nome, Perfil perfil) {
        pessoa(pessoa, nome);
        Usuario usuario = new Usuario();
        usuario.setPessoa(pessoa);
        usuario.setSenha(passwordEncoder.encode(SENHA));
        usuario.setPerfil(perfil);
        entityManager.persist(usuario);
        pessoa.setUsuario(usuario);
        return pessoa;
    }

    private <T extends Pessoa> T pessoa(T pessoa, String nome) {
        String cpf = "%011d".formatted(40000000000L + SEQUENCIA.incrementAndGet());
        pessoa.setNome(nome);
        pessoa.setCpf(cpf);
        pessoa.setDataNascimento(LocalDate.of(1990, 1, 1));
        pessoa.setSexo('F');
        pessoa.setEstadoCivil(EstadoCivil.SOLTEIRO);
        pessoa.setEmail(cpf + "@email.com");
        if (pessoa instanceof Profissional profissional) {
            profissional.setCargo(cargo);
        }
        if (pessoa instanceof Especialista especialista) {
            especialista.setRegistroConselho("CRP-" + cpf);
        }
        entityManager.persist(pessoa);
        return pessoa;
    }
}
