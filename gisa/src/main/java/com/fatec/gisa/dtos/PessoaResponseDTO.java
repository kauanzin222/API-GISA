package com.fatec.gisa.dtos;

import java.time.LocalDate;
import java.util.List;

import com.fatec.gisa.enums.EstadoCivil;
import com.fatec.gisa.enums.StatusCadastro;

public record PessoaResponseDTO(
        Long idCadastro,
        String nome,
        String cpf,
        LocalDate dataNascimento,
        Character sexo,
        String celular,
        String numCNS,
        EstadoCivil estadoCivil,
        StatusCadastro statusCadastro,
        List<EnderecoDTO> enderecos) {
}