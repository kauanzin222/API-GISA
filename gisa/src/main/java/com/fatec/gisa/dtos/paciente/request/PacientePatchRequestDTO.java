package com.fatec.gisa.dtos.paciente.request;

import java.util.List;

import com.fatec.gisa.dtos.PessoaPatchRequestDTO;
import com.fatec.gisa.enums.StatusPaciente;
import com.fasterxml.jackson.annotation.JsonSetter;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PacientePatchRequestDTO extends PessoaPatchRequestDTO {

    private StatusPaciente statusPaciente;
    private Boolean convenio;
    private Long idEscola;
    private List<String> codigosCid;
    private List<@Valid CidCadastroRequestDTO> novosCids;
    private ProntuarioPatchRequestDTO prontuario;
    private List<@Valid ResponsavelVinculoPatchDTO> responsaveis;

    @JsonSetter("statusPaciente")
    public void setStatusPaciente(StatusPaciente statusPaciente) {
        this.statusPaciente = statusPaciente;
        markProvided("statusPaciente");
    }

    @JsonSetter("convenio")
    public void setConvenio(Boolean convenio) {
        this.convenio = convenio;
        markProvided("convenio");
    }

    @JsonSetter("idEscola")
    public void setIdEscola(Long idEscola) {
        this.idEscola = idEscola;
        markProvided("idEscola");
    }

    @JsonSetter("codigosCid")
    public void setCodigosCid(List<String> codigosCid) {
        this.codigosCid = codigosCid;
        markProvided("codigosCid");
    }

    @JsonSetter("novosCids")
    public void setNovosCids(List<@Valid CidCadastroRequestDTO> novosCids) {
        this.novosCids = novosCids;
        markProvided("novosCids");
    }

    @JsonSetter("prontuario")
    public void setProntuario(ProntuarioPatchRequestDTO prontuario) {
        this.prontuario = prontuario;
        markProvided("prontuario");
    }

    @JsonSetter("responsaveis")
    public void setResponsaveis(List<@Valid ResponsavelVinculoPatchDTO> responsaveis) {
        this.responsaveis = responsaveis;
        markProvided("responsaveis");
    }
}
