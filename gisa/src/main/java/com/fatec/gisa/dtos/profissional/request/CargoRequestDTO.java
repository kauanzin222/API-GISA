package com.fatec.gisa.dtos.profissional.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CargoRequestDTO {

    @NotBlank(message = "O nome do cargo é obrigatório")
    private String nome;

    @NotBlank(message = "O código CBO é obrigatório")
    private String codigoCBO;
}
