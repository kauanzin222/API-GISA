package com.fatec.gisa.dtos.especialista.request;

import java.util.List;

import com.fatec.gisa.dtos.especialista.JornadaTrabalhoDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalCadastroRequestDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class EspecialistaCadastroRequestDTO extends ProfissionalCadastroRequestDTO {

    @NotBlank(message = "O registro no conselho é obrigatório")
    private String registroConselho;

    // IDs de especialidades que já existem no banco
    private List<Long> especialidadesIds;

    // Novas especialidades criadas na tela pelo usuário
    @Valid
    private List<EspecialidadeRequestDTO> novasEspecialidades;

    @Valid
    @NotEmpty(message = "Informe ao menos um horário de jornada de trabalho")
    private List<JornadaTrabalhoDTO> jornadas;
}
