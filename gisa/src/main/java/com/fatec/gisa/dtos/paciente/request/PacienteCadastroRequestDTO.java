package com.fatec.gisa.dtos.paciente.request;

import java.util.List;

import com.fatec.gisa.dtos.EnderecoDTO;
import com.fatec.gisa.dtos.PessoaCadastroRequestDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PacienteCadastroRequestDTO extends PessoaCadastroRequestDTO {

    @Positive(message = "O ID da escola deve ser positivo")
    private Long idEscola;

    private List<@NotBlank(message = "O código CID não pode ser vazio") String> codigosCid;

    @NotNull(message = "Os dados do prontuário são obrigatórios")
    @Valid
    private ProntuarioCadastroRequestDTO prontuario;

    @NotEmpty(message = "Informe ao menos um responsável")
    @Valid
    private List<ResponsavelCadastroRequestDTO> responsaveis;

    @Override
    @NotEmpty(message = "Informe ao menos um endereço para o paciente")
    public List<EnderecoDTO> getEnderecos() {
        return super.getEnderecos();
    }
}