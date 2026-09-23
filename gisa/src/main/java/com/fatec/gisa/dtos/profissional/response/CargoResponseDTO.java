package com.fatec.gisa.dtos.profissional.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CargoResponseDTO {

    private Long idCargo;
    private String nome;
    private String codigoCBO;
    private String tituloCBO;
}
