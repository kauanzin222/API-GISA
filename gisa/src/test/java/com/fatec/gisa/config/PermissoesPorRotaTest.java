package com.fatec.gisa.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.usuario.Perfil;
import com.fatec.gisa.entities.usuario.Usuario;
import com.fatec.gisa.enums.EstadoCivil;
import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManager;

// Garante que toda rota da API exige permissão, exceto as que só pedem login. Uma rota nova
// sem @PreAuthorize faz este teste falhar.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:permissoes-rota;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef"
})
@AutoConfigureMockMvc
class PermissoesPorRotaTest {

    private static final String SENHA = "SenhaInicial1";
    private static final String CPF = "30000000001";

    // Rotas sem permissão específica (ver docs/00-GERAL.md)
    private static final Set<String> SO_LOGIN = Set.of(
            "POST /api/auth/login",
            "GET /api/usuarios/me",
            "PATCH /api/usuarios/minha-senha",
            "GET /api/terapias/minhas",
            "GET /api/especialidades");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapeamentos;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void criarUsuarioSemPermissoes() {
        transactionTemplate.executeWithoutResult(status -> {
            if (!entityManager.createQuery("select p from Pessoa p where p.cpf = :cpf")
                    .setParameter("cpf", CPF).getResultList().isEmpty()) {
                return;
            }
            Perfil perfil = new Perfil(null, "Sem permissões", new ArrayList<>());
            entityManager.persist(perfil);
            Pessoa pessoa = new Pessoa();
            pessoa.setNome("Sem Permissões");
            pessoa.setCpf(CPF);
            pessoa.setDataNascimento(LocalDate.of(1990, 1, 1));
            pessoa.setSexo('F');
            pessoa.setEstadoCivil(EstadoCivil.SOLTEIRO);
            entityManager.persist(pessoa);
            Usuario usuario = new Usuario();
            usuario.setPessoa(pessoa);
            usuario.setSenha(passwordEncoder.encode(SENHA));
            usuario.setPerfil(perfil);
            entityManager.persist(usuario);
        });
    }

    @Test
    void todaRotaForaDaListaSoLoginTemPreAuthorize() {
        List<String> semPermissao = new ArrayList<>();
        List<String> soLoginEncontradas = new ArrayList<>();

        rotas().forEach((rota, metodo) -> {
            boolean protegida = AnnotatedElementUtils.hasAnnotation(metodo.getMethod(), PreAuthorize.class)
                    || AnnotatedElementUtils.hasAnnotation(metodo.getBeanType(), PreAuthorize.class);
            if (SO_LOGIN.contains(rota)) {
                soLoginEncontradas.add(rota);
            } else if (!protegida) {
                semPermissao.add(rota);
            }
        });

        assertThat(rotas()).as("rotas da API").hasSizeGreaterThan(70);
        assertThat(semPermissao).as("rotas sem @PreAuthorize").isEmpty();
        assertThat(soLoginEncontradas).as("rotas só com login").containsExactlyInAnyOrderElementsOf(SO_LOGIN);
    }

    // Rotas com corpo ficam de fora: o corpo é validado antes da permissão, então um "{}" daria 400
    @Test
    void usuarioSemPermissoesRecebe403EmTodaRotaSemCorpo() throws Exception {
        String token = login();
        List<String> naoBloqueadas = new ArrayList<>();
        int verificadas = 0;

        for (Map.Entry<String, HandlerMethod> entrada : rotas().entrySet()) {
            String rota = entrada.getKey();
            boolean temCorpo = java.util.Arrays.stream(entrada.getValue().getMethodParameters())
                    .anyMatch(parametro -> parametro.hasParameterAnnotation(RequestBody.class));
            if (SO_LOGIN.contains(rota) || temCorpo) {
                continue;
            }
            verificadas++;
            String[] partes = rota.split(" ", 2);
            String url = partes[1].replaceAll("\\{[^}]+}", "1");
            var resposta = mockMvc.perform(request(HttpMethod.valueOf(partes[0]), url)
                            .header("Authorization", "Bearer " + token))
                    .andReturn().getResponse();
            if (resposta.getStatus() != 403
                    || !resposta.getContentAsString().contains("Você não tem permissão para realizar esta ação.")) {
                naoBloqueadas.add(rota + " -> " + resposta.getStatus());
            }
        }

        assertThat(verificadas).as("rotas verificadas").isGreaterThanOrEqualTo(39);
        assertThat(naoBloqueadas).isEmpty();
    }

    // "GET /api/pacientes/{idCadastro}" -> método do controller
    private Map<String, HandlerMethod> rotas() {
        Map<String, HandlerMethod> rotas = new java.util.TreeMap<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entrada : mapeamentos.getHandlerMethods().entrySet()) {
            for (String caminho : entrada.getKey().getPatternValues()) {
                if (!caminho.startsWith("/api/")) {
                    continue;
                }
                entrada.getKey().getMethodsCondition().getMethods()
                        .forEach(metodo -> rotas.put(metodo.name() + " " + caminho, entrada.getValue()));
            }
        }
        return rotas;
    }

    private String login() throws Exception {
        String resposta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"cpf\": \"%s\", \"senha\": \"%s\" }".formatted(CPF, SENHA)))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.token");
    }
}
