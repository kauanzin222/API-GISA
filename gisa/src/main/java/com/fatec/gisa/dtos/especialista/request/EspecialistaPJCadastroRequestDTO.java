package com.fatec.gisa.dtos.especialista.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class EspecialistaPJCadastroRequestDTO extends EspecialistaCadastroRequestDTO {

    @NotBlank(message = "O CNPJ é obrigatório")
    @Pattern(regexp = "\\d{14}", message = "O CNPJ deve conter 14 dígitos numéricos")
    private String cnpj;

    @NotBlank(message = "A razão social é obrigatória")
    private String razaoSocial;

    private String nomeFantasia;

    private String inscricaoEstadual;
}
