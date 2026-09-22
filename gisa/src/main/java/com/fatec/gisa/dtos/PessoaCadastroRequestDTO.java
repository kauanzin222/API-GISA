package com.fatec.gisa.dtos;

import java.time.LocalDate;
import java.util.List;

import com.fatec.gisa.enums.EstadoCivil;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PessoaCadastroRequestDTO {

    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    @NotBlank(message = "O CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "O CPF deve conter 11 dígitos numéricos")
    private String cpf;

    private String rg;

    @NotNull(message = "A data de nascimento é obrigatória")
    private LocalDate dataNascimento;

    private Character sexo;

    @NotBlank(message = "O celular é obrigatório")
    private String celular;

    private String numCNS;

    private EstadoCivil estadoCivil;

    @Valid
    private List<EnderecoDTO> enderecos;
}
