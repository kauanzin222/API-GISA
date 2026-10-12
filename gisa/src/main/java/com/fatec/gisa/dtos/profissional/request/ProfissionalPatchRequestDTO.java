package com.fatec.gisa.dtos.profissional.request;

import com.fatec.gisa.dtos.PessoaPatchRequestDTO;
import com.fasterxml.jackson.annotation.JsonSetter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProfissionalPatchRequestDTO extends PessoaPatchRequestDTO {

    @Email(message = "Formato de e-mail inválido")
    private String email;
    @Positive(message = "O ID do cargo deve ser positivo")
    private Long idCargo;
    @Positive(message = "O ID do perfil deve ser positivo")
    private Long idPerfil;

    @JsonSetter("email")
    public void setEmail(String email) {
        this.email = email;
        markProvided("email");
    }

    @JsonSetter("idCargo")
    public void setIdCargo(Long idCargo) {
        this.idCargo = idCargo;
        markProvided("idCargo");
    }

    @JsonSetter("idPerfil")
    public void setIdPerfil(Long idPerfil) {
        this.idPerfil = idPerfil;
        markProvided("idPerfil");
    }
}
