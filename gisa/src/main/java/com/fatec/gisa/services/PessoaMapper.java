package com.fatec.gisa.services;

import org.springframework.stereotype.Component;

import com.fatec.gisa.dtos.PessoaCadastroRequestDTO;
import com.fatec.gisa.entities.Pessoa;

@Component 
public class PessoaMapper {
    public void preencherPessoa(Pessoa pessoa, PessoaCadastroRequestDTO dto) {
        pessoa.setNome(dto.getNome());
        pessoa.setCpf(dto.getCpf());
        pessoa.setDataNascimento(dto.getDataNascimento());
        pessoa.setSexo(dto.getSexo());
        pessoa.setCelular(dto.getCelular());
        pessoa.setNumCNS(dto.getNumCNS());
        pessoa.setEstadoCivil(dto.getEstadoCivil());
    }
}
