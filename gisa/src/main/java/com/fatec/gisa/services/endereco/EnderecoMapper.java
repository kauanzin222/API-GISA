package com.fatec.gisa.services.endereco;

import java.util.ArrayList;

import org.springframework.stereotype.Component;

import com.fatec.gisa.dtos.EnderecoDTO;
import com.fatec.gisa.entities.Endereco;

@Component
public class EnderecoMapper {
    public Endereco criarEndereco(EnderecoDTO dto) {
        Endereco endereco = new Endereco();
        endereco.setCep(dto.getCep());
        endereco.setRua(dto.getRua());
        endereco.setNumero(dto.getNumero());
        endereco.setComplemento(dto.getComplemento());
        endereco.setBairro(dto.getBairro());
        endereco.setCidade(dto.getCidade());
        endereco.setEstado(dto.getEstado());
        endereco.setMoradores(new ArrayList<>());
        return endereco;
    }
}
