package com.fatec.gisa.dtos;

// Filtro das listagens de pessoas. Valores vazios não restringem a busca.
public record FiltroPessoaDTO(String cpf, String nome) {

    public static FiltroPessoaDTO de(String cpf, String nome) {
        return new FiltroPessoaDTO(
                cpf == null ? "" : cpf.replaceAll("\\D", ""),
                nome == null ? "" : nome.trim());
    }
}
