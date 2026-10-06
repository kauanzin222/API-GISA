package com.fatec.gisa.services.profissional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.fatec.gisa.entities.especialista.EspecialistaPJ;
import com.fatec.gisa.services.PessoaMapper;

class ProfissionalMapperTest {

    @Test
    void mapsEspecialistaPJWithCommonAndCompanyDataSeparated() {
        ProfissionalMapper mapper = new ProfissionalMapper(new PessoaMapper());
        EspecialistaPJ especialistaPJ = new EspecialistaPJ();
        especialistaPJ.setIdCadastro(42L);
        especialistaPJ.setNome("Ana Oliveira");
        especialistaPJ.setCpf("34567890123");
        especialistaPJ.setEmail("ana@example.com");
        especialistaPJ.setRegistroConselho("CRM-123");
        especialistaPJ.setCnpj("12345678000195");
        especialistaPJ.setRazaoSocial("Clinica Exemplo Ltda");

        var response = mapper.toEspecialistaPJResponseDTO(especialistaPJ);

        assertEquals("12345678000195", response.cnpj());
        assertEquals("Clinica Exemplo Ltda", response.razaoSocial());
        assertEquals("CRM-123", response.especialista().registroConselho());
        assertNotNull(response.especialista().profissional().pessoa());
        assertEquals(42L, response.especialista().profissional().pessoa().idCadastro());
        assertEquals("Ana Oliveira", response.especialista().profissional().pessoa().nome());
    }
}