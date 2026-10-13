package com.fatec.gisa.dtos.profissional.response;

import java.util.List;

public record CboDetalheDTO(
        String codigoCBO,
        String tituloCBO,
        List<CargoResumoDTO> cargos) {
}
