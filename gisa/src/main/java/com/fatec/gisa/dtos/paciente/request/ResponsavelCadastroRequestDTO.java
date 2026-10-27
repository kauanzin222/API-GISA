package com.fatec.gisa.dtos.paciente.request;

import com.fatec.gisa.dtos.PessoaCadastroRequestDTO;
import com.fatec.gisa.enums.GrauParentesco;

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

    // false: o responsável fica com os endereços do paciente (os enviados para ele são ignorados)
    @NotNull(message = "Informe se o responsável não reside com o paciente")
    private Boolean naoResideComPaciente;
}
