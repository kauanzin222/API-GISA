package com.fatec.gisa.services.cadastro;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fatec.gisa.dtos.EnderecoDTO;
import com.fatec.gisa.dtos.PessoaCadastroRequestDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalCadastroRequestDTO;
import com.fatec.gisa.entities.Endereco;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Profissional;

@Component
public class CadastroMapper {
    public void preencherPessoa(Pessoa pessoa, PessoaCadastroRequestDTO dto) {
        pessoa.setNome(dto.getNome());
        pessoa.setCpf(dto.getCpf());
        pessoa.setDataNascimento(dto.getDataNascimento());
        pessoa.setSexo(dto.getSexo());
        pessoa.setCelular(dto.getCelular());
        pessoa.setNumCNS(dto.getNumCNS());
        pessoa.setEstadoCivil(dto.getEstadoCivil());
    }

    public void preencherProfissional(Profissional profissional,
            ProfissionalCadastroRequestDTO dto, Cargo cargo) {
        preencherPessoa(profissional, dto);
        profissional.setEmail(dto.getEmail());
        profissional.setCargo(cargo);
    }

    public List<Endereco> mapearEnderecos(Pessoa pessoa, List<EnderecoDTO> dtos) {
        return dtos.stream().map(dto -> {
            Endereco endereco = new Endereco();
            endereco.setCep(dto.getCep());
            endereco.setRua(dto.getRua());
            endereco.setNumero(dto.getNumero());
            endereco.setComplemento(dto.getComplemento());
            endereco.setBairro(dto.getBairro());
            endereco.setCidade(dto.getCidade());
            endereco.setEstado(dto.getEstado());
            endereco.setMoradores(List.of(pessoa));
            return endereco;
        }).collect(Collectors.toList());
    }
}
