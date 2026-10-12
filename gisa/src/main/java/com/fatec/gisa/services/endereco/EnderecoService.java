package com.fatec.gisa.services.endereco;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.EnderecoDTO;
import com.fatec.gisa.entities.Endereco;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.repositories.EnderecoRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final EnderecoMapper enderecoMapper;

    @Transactional
    public void associarEnderecos(Pessoa pessoa, List<EnderecoDTO> enderecosDTO) {
        if (enderecosDTO == null || enderecosDTO.isEmpty()) {
            pessoa.setEnderecos(List.of());
            return;
        }

        List<Endereco> enderecos = new ArrayList<>();
        for (EnderecoDTO dto : enderecosDTO) {
            Endereco endereco = enderecoRepository.findByCepAndNumeroAndComplemento(dto.getCep(), dto.getNumero(), dto.getComplemento())
                    .orElseGet(() -> enderecoMapper.criarEndereco(dto));

            if (!endereco.getMoradores().contains(pessoa)) {
                endereco.getMoradores().add(pessoa);
            }

            enderecos.add(endereco);
        }

        enderecoRepository.saveAll(enderecos);
        pessoa.setEnderecos(enderecos);
    }

    @Transactional
    public void substituirEnderecos(Pessoa pessoa, List<EnderecoDTO> enderecosDTO) {
        List<Endereco> enderecosAnteriores = pessoa.getEnderecos() == null
                ? List.of()
                : new ArrayList<>(pessoa.getEnderecos());

        if (enderecosDTO == null || enderecosDTO.isEmpty()) {
            desvincularPessoa(pessoa, enderecosAnteriores);
            pessoa.setEnderecos(List.of());
            return;
        }

        List<Endereco> enderecos = new ArrayList<>();
        for (EnderecoDTO dto : enderecosDTO) {
            Endereco endereco = enderecoRepository.findByCepAndNumeroAndComplemento(
                    dto.getCep(), dto.getNumero(), dto.getComplemento())
                    .orElseGet(() -> enderecoMapper.criarEndereco(dto));
            if (!endereco.getMoradores().contains(pessoa)) {
                endereco.getMoradores().add(pessoa);
            }
            if (!enderecos.contains(endereco)) {
                enderecos.add(endereco);
            }
        }

        desvincularPessoa(pessoa, enderecosAnteriores.stream()
                .filter(endereco -> !enderecos.contains(endereco))
                .toList());
        enderecoRepository.saveAll(enderecos);
        pessoa.setEnderecos(enderecos);
    }

    private void desvincularPessoa(Pessoa pessoa, List<Endereco> enderecos) {
        for (Endereco endereco : enderecos) {
            endereco.getMoradores().remove(pessoa);
        }
        enderecoRepository.saveAll(enderecos);
    }
}
