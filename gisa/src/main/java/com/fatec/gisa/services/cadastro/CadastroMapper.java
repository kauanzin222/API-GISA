package com.fatec.gisa.services.cadastro;

import org.springframework.stereotype.Component;

import com.fatec.gisa.dtos.PessoaCadastroRequestDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalCadastroRequestDTO;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Profissional;

@Component
public class CadastroMapper {
    public void preencherPessoa(Pessoa pessoa, PessoaCadastroRequestDTO dto) {
        pessoa.setNome(dto.getNome());
        pessoa.setCpf(dto.getCpf());
        pessoa.setRg(dto.getRg());
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
}
