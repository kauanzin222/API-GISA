package com.fatec.gisa.dtos.paciente.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CidCadastroRequestDTO {

    @NotBlank(message = "O código CID é obrigatório")
    private String codigoCID;

    @NotBlank(message = "A descrição do CID é obrigatória")
    private String descricao;
}