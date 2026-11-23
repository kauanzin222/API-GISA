package com.fatec.gisa.dtos.paciente.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fatec.gisa.dtos.PessoaCadastroRequestDTO;
import com.fatec.gisa.enums.GrauParentesco;

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

    // POST /pacientes: posições, na lista "enderecos" do próprio request, dos endereços do paciente em que o
    // responsável mora. [] = não reside
    private List<Integer> indicesEnderecoResidencia;

    // POST /pacientes/{id}/responsaveis: IDs dos endereços atuais do paciente em que o responsável mora.
    // [] = não reside
    private List<Long> idsEnderecoResidencia;

    // Só por compatibilidade (usado quando as duas listas acima não vêm): false = mora em todos os endereços do
    // paciente (os enviados para ele são ignorados); true = não reside
    private Boolean naoResideComPaciente;

    @JsonIgnore
    @AssertTrue(message = "Informe onde o responsável reside ou se ele não reside com o paciente")
    public boolean isResidenciaInformada() {
        return indicesEnderecoResidencia != null || idsEnderecoResidencia != null || naoResideComPaciente != null;
    }
}
