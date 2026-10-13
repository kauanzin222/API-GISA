package com.fatec.gisa.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// Origens lidas de cors.allowed-origins (CORS_ORIGINS), com espaços e itens vazios ignorados
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cors;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef",
        "cors.allowed-origins= https://gisa.exemplo.com , ,https://homologa.exemplo.com"
})
@AutoConfigureMockMvc
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void liberaAsOrigensConfiguradas() throws Exception {
        for (String origem : new String[] { "https://gisa.exemplo.com", "https://homologa.exemplo.com" }) {
            preflight(origem)
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origem));
        }
    }

    @Test
    void recusaOrigemForaDaLista() throws Exception {
        // Com a variável definida, o padrão localhost:4200 deixa de valer
        preflight("http://localhost:4200").andExpect(status().isForbidden());
        preflight("https://outro.exemplo.com").andExpect(status().isForbidden());
    }

    private ResultActions preflight(String origem) throws Exception {
        return mockMvc.perform(options("/api/pacientes")
                .header("Origin", origem)
                .header("Access-Control-Request-Method", "GET"));
    }
}
