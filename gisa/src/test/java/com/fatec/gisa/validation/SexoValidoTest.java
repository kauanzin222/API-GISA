package com.fatec.gisa.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fatec.gisa.dtos.PessoaCadastroRequestDTO;

import jakarta.validation.Validation;

class SexoValidoTest {

    @Test
    void aceitaSomenteFMeO() {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var validator = validatorFactory.getValidator();

            assertTrue(validator.validateValue(PessoaCadastroRequestDTO.class, "sexo", 'F').isEmpty());
            assertTrue(validator.validateValue(PessoaCadastroRequestDTO.class, "sexo", 'M').isEmpty());
            assertTrue(validator.validateValue(PessoaCadastroRequestDTO.class, "sexo", 'O').isEmpty());

            var violations = validator.validateValue(PessoaCadastroRequestDTO.class, "sexo", 'X');
            assertEquals(1, violations.size());
            assertEquals("O sexo deve ser F, M ou O", violations.iterator().next().getMessage());
        }
    }
}
