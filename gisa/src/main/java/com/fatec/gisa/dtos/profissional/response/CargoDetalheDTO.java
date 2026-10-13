package com.fatec.gisa.dtos.profissional.response;

import java.util.List;

import com.fatec.gisa.dtos.PessoaVinculadaDTO;

public record CargoDetalheDTO(
        Long idCargo,
        String nome,
        String codigoCBO,
        String tituloCBO,
        List<PessoaVinculadaDTO> profissionais) {
}
