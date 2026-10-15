package com.fatec.gisa.dtos.assistentesocial.request;

import java.util.List;

import com.fatec.gisa.dtos.profissional.request.ProfissionalCadastroRequestDTO;

import jakarta.validation.constraints.NotBlank;
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
public class AssistenteSocialCadastroRequestDTO extends ProfissionalCadastroRequestDTO {

    // Número do CRESS
    @NotBlank(message = "O registro no conselho é obrigatório")
    private String registroConselho;

    // Opcional: pacientes já vinculados no cadastro. IDs repetidos são unificados
    private List<@NotNull(message = "O ID do paciente não pode ser nulo")
            @Positive(message = "O ID do paciente deve ser positivo") Long> idsPacientes;
}
