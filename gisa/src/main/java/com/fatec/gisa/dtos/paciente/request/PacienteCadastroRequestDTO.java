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

    private Boolean convenio;

    private List<@NotBlank(message = "O código CID não pode ser vazio") String> codigosCid;

    private List<@Valid CidCadastroRequestDTO> novosCids;

    @NotNull(message = "Os dados do prontuário são obrigatórios")
    @Valid
    private ProntuarioCadastroRequestDTO prontuario;

    @NotEmpty(message = "Informe ao menos um responsável")
    private List<@Valid ResponsavelCadastroRequestDTO> responsaveis;

    @Override
    @NotEmpty(message = "Informe ao menos um endereço para o paciente")
    public List<EnderecoDTO> getEnderecos() {
        return super.getEnderecos();
    }
}