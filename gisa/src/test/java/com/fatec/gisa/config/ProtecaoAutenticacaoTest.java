package com.fatec.gisa.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.SecretKey;

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
import com.fatec.gisa.entities.usuario.Usuario;
import com.fatec.gisa.enums.EstadoCivil;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManager;

// Token ausente ou inválido (401 com corpo, nunca 500) e limite de tentativas por e-mail
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:protecao-autenticacao;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=" + ProtecaoAutenticacaoTest.SEGREDO,
        "gisa.auth.max-falhas-login=3",
        "gisa.auth.max-pedidos-esqueci-senha=2"
})
@AutoConfigureMockMvc
class ProtecaoAutenticacaoTest {

    static final String SEGREDO = "0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String SENHA = "SenhaInicial1";
    private static final String SESSAO_INVALIDA = "Sessão expirada ou inválida. Faça login novamente.";
    private static final AtomicInteger SEQUENCIA = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String email;
    private Long idUsuario;

    // E-mail novo a cada teste: o limite fica em memória e vale para o contexto inteiro
    @BeforeEach
    void preparar() {
        int numero = SEQUENCIA.incrementAndGet();
        email = "usuario" + numero + "@gisa.test";
        idUsuario = transactionTemplate.execute(status -> {
            Perfil perfil = new Perfil(null, "Comum" + numero, new ArrayList<>());
            entityManager.persist(perfil);

            Pessoa pessoa = new Pessoa();
            pessoa.setNome("Pessoa " + numero);
            pessoa.setCpf("%011d".formatted(60000000000L + numero));
            pessoa.setEmail(email);
            pessoa.setDataNascimento(LocalDate.of(1990, 1, 1));
            pessoa.setSexo('F');
            pessoa.setEstadoCivil(EstadoCivil.SOLTEIRO);
            entityManager.persist(pessoa);

            Usuario usuario = new Usuario();
            usuario.setPessoa(pessoa);
            usuario.setSenha(passwordEncoder.encode(SENHA));
            usuario.setPerfil(perfil);
            entityManager.persist(usuario);
            return usuario.getIdCadastro();
        });
    }

    @Test
    void semTokenResponde401ComCorpo() throws Exception {
        mockMvc.perform(get("/api/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SESSAO_INVALIDA))
                .andExpect(jsonPath("$.path").value("/api/usuarios/me"));
    }

    @Test
    void tokenMalformadoAssinadoComOutraChaveOuExpiradoResponde401() throws Exception {
        SecretKey outraChave = Keys.hmacShaKeyFor("fedcba9876543210fedcba9876543210fedcba9876543210".getBytes());
        SecretKey chave = Keys.hmacShaKeyFor(SEGREDO.getBytes());
        String cpf = "%011d".formatted(60000000000L + SEQUENCIA.get());
        long agora = System.currentTimeMillis();

        for (String token : new String[] {
                "nao-e-um-jwt",
                Jwts.builder().subject(cpf).signWith(outraChave).compact(),
                Jwts.builder().subject(cpf).issuedAt(new Date(agora - 120_000))
                        .expiration(new Date(agora - 60_000)).signWith(chave).compact() }) {
            mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(SESSAO_INVALIDA));
        }
    }

    @Test
    void bloqueiaOLoginDoEMailDepoisDeMuitasFalhas() throws Exception {
        for (int i = 0; i < 3; i++) {
            login(email, "errada").andExpect(status().isUnauthorized());
        }

        // Bloqueado mesmo com a senha certa, e sem diferenciar maiúsculas no e-mail
        login(email.toUpperCase(), SENHA)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(
                        "Muitas tentativas de login sem sucesso. Aguarde alguns minutos e tente novamente."));
        // Outro e-mail não é afetado
        login("outro" + email, "errada").andExpect(status().isUnauthorized());
    }

    @Test
    void loginCertoZeraAsFalhas() throws Exception {
        login(email, "errada").andExpect(status().isUnauthorized());
        login(email, "errada").andExpect(status().isUnauthorized());
        login(email, SENHA).andExpect(status().isOk());

        login(email, "errada").andExpect(status().isUnauthorized());
        login(email, "errada").andExpect(status().isUnauthorized());
        login(email, SENHA).andExpect(status().isOk());
    }

    @Test
    void esqueciSenhaAcimaDoLimiteResponde204SemGerarNovoToken() throws Exception {
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/esqueci-senha")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"email\": \"%s\" }".formatted(email)))
                    .andExpect(status().isNoContent());
        }

        Long tokens = transactionTemplate.execute(status -> entityManager.createQuery(
                "select count(t) from TokenRedefinicaoSenha t where t.idUsuario = :id", Long.class)
                .setParameter("id", idUsuario)
                .getSingleResult());
        assertThat(tokens).isEqualTo(2);
    }

    private ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"%s\", \"senha\": \"%s\" }".formatted(email, senha)));
    }
}
