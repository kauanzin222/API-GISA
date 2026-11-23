package com.fatec.gisa.services;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fatec.gisa.dtos.EnderecoDTO;
import com.fatec.gisa.dtos.PessoaCadastroRequestDTO;
import com.fatec.gisa.dtos.PessoaResponseDTO;
import com.fatec.gisa.entities.Endereco;
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

    public PessoaResponseDTO toPessoaResponseDTO(Pessoa pessoa) {
        List<EnderecoDTO> enderecos = pessoa.getEnderecos() == null
                ? List.of()
                : pessoa.getEnderecos().stream()
                        // O primeiro (menor ID) é o principal; List de @ManyToMany não garante ordem
                        .sorted(Comparator.comparing(Endereco::getIdEndereco,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(this::toEnderecoDTO)
                        .toList();

        return new PessoaResponseDTO(
                pessoa.getIdCadastro(),
                pessoa.getNome(),
                pessoa.getCpf(),
                pessoa.getDataNascimento(),
                pessoa.getSexo(),
                pessoa.getCelular(),
                pessoa.getNumCNS(),
                pessoa.getEstadoCivil(),
                pessoa.getStatusCadastro(),
                enderecos);
    }

    private EnderecoDTO toEnderecoDTO(Endereco endereco) {
        return new EnderecoDTO(
                endereco.getIdEndereco(),
                endereco.getCep(),
                endereco.getRua(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getEstado(),
                null);
    }
}
