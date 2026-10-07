package com.fatec.gisa.dtos.paciente.request;

import com.fatec.gisa.enums.TipoEscola;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscolaCadastroRequestDTO {

    @NotBlank(message = "O nome da escola é obrigatório")
    private String nome;

    @NotNull(message = "O tipo da escola é obrigatório")
    private TipoEscola tipoEscola;

    private String telefone;
}