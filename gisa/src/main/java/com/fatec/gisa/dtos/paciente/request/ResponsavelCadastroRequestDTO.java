package com.fatec.gisa.dtos.paciente.request;

import com.fatec.gisa.dtos.PessoaCadastroRequestDTO;
import com.fatec.gisa.enums.GrauParentesco;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ResponsavelCadastroRequestDTO extends PessoaCadastroRequestDTO {

    @Positive(message = "O ID do responsável deve ser positivo")
    private Long idResponsavel;

    private String ocupacao;

    @NotNull(message = "O grau de parentesco é obrigatório")
    private GrauParentesco grauParentesco;

    @NotNull(message = "Informe se o responsável não reside com o paciente")
    private Boolean naoResideComPaciente;

    @JsonIgnore
    @AssertTrue(message = "Informe ao menos um endereço para o responsável que não reside com o paciente")
    public boolean isEnderecoAlternativoValido() {
        return !Boolean.TRUE.equals(naoResideComPaciente)
            || (getEnderecos() != null && !getEnderecos().isEmpty());
    }
}