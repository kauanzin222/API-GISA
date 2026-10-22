package com.fatec.gisa.dtos.paciente.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CidCadastroRequestDTO {

    // Formato do CID-10: letra maiúscula, dois dígitos e, opcionalmente, ponto e um dígito (F84 ou F84.0)
    @NotBlank(message = "O código CID é obrigatório")
    @Pattern(regexp = "^[A-Z][0-9]{2}(\\.[0-9])?$",
            message = "Código CID inválido. Use o formato do CID-10, como F84 ou F84.0")
    private String codigoCID;

    @NotBlank(message = "A descrição do CID é obrigatória")
    @Size(max = 255, message = "A descrição do CID deve ter no máximo 255 caracteres")
    private String descricao;
}
