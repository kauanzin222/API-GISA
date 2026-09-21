package com.fatec.gisa.dtos.profissional.request;

import com.fatec.gisa.dtos.PessoaCadastroRequestDTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProfissionalCadastroRequestDTO extends PessoaCadastroRequestDTO {

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "Formato de e-mail inválido")
    private String email;

    @NotNull(message = "O ID do cargo é obrigatório")
    private Long idCargo;
}
