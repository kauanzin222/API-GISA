package com.fatec.gisa.services.endereco;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.EnderecoDTO;
import com.fatec.gisa.entities.Endereco;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.repositories.EnderecoRepository;
import com.fatec.gisa.services.cadastro.CadastroMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final CadastroMapper cadastroMapper;

    @Transactional
    public void associarEnderecos(Pessoa pessoa, List<EnderecoDTO> enderecosDTO) {
        if (enderecosDTO == null || enderecosDTO.isEmpty()) {
            pessoa.setEnderecos(List.of());
            return;
        }

        List<Endereco> enderecos = new ArrayList<>();

        for (EnderecoDTO dto : enderecosDTO) {
            Endereco endereco = enderecoRepository.findByCepAndNumeroAndComplemento(dto.getCep(), dto.getNumero(), dto.getComplemento())
                    .orElseGet(() -> cadastroMapper.criarEndereco(dto));

            if (!endereco.getMoradores().contains(pessoa)) {
                endereco.getMoradores().add(pessoa);
            }

            enderecos.add(endereco);
        }

        enderecoRepository.saveAll(enderecos);
        pessoa.setEnderecos(enderecos);
    }
}
