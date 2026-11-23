package com.fatec.gisa.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnderecoDTO {

    // Resposta: sempre. Request: ausente = endereço novo; presente = um endereço atual da pessoa
    private Long idEndereco;

    @NotBlank(message = "O CEP é obrigatório")
    private String cep;

    @NotBlank(message = "A rua é obrigatório")
    private String rua;

    @NotBlank(message = "O número é obrigatório")
    private String numero;

    private String complemento;

    @NotBlank(message = "O bairro é obrigatório")
    private String bairro;

    @NotBlank(message = "A cidade é obrigatória")
    private String cidade;

    @NotBlank(message = "O estado é obrigatório")
    private String estado;

    // Só no request: se a alteração de um endereço que outros cadastros também usam vale para eles
    // (true) ou só para quem está sendo editado (false). Ver EnderecoService.sincronizar
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Boolean alterarParaTodos;

    public EnderecoDTO(String cep, String rua, String numero, String complemento, String bairro, String cidade,
            String estado) {
        this(null, cep, rua, numero, complemento, bairro, cidade, estado, null);
    }
}
