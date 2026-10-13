package com.fatec.gisa;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Banco H2 em memória, para o teste não depender do banco de produção (DB_HOST etc.).
// As migrations são validadas contra PostgreSQL real em ApiPostgresTest.
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:contexto;MODE=PostgreSQL",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef"
})
class GisaApplicationTests {

	@Test
	void contextLoads() {
	}

}
