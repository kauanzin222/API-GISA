package com.fatec.gisa.services.endereco;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.EnderecoDTO;
import com.fatec.gisa.entities.Endereco;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.repositories.EnderecoRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

// Um endereço é compartilhado por quem mora nele (CEP + número + complemento identificam o endereço)
// e só existe enquanto tiver morador: quem fica sem ninguém é excluído
@Service
@RequiredArgsConstructor
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final EnderecoMapper enderecoMapper;

    @Transactional
    public void associarEnderecos(Pessoa pessoa, List<EnderecoDTO> enderecosDTO) {
        substituirEnderecos(pessoa, enderecosDTO);
    }

    @Transactional
    public void substituirEnderecos(Pessoa pessoa, List<EnderecoDTO> enderecosDTO) {
        List<Endereco> enderecos = new ArrayList<>();
        if (enderecosDTO != null) {
            for (EnderecoDTO dto : enderecosDTO) {
                Endereco endereco = obterOuCriar(dto);
                if (!enderecos.contains(endereco)) {
                    enderecos.add(endereco);
                }
            }
        }
        vincular(pessoa, enderecos);
    }

    // A pessoa passa a ter exatamente os endereços de outra (ex.: responsável que reside com o paciente)
    @Transactional
    public void igualarEnderecos(Pessoa pessoa, Pessoa origem) {
        vincular(pessoa, origem.getEnderecos() == null ? List.of() : new ArrayList<>(origem.getEnderecos()));
    }

    public boolean mesmosEnderecos(Pessoa uma, Pessoa outra) {
        return ids(uma).equals(ids(outra));
    }

    public boolean temEndereco(Pessoa pessoa) {
        return pessoa.getEnderecos() != null && !pessoa.getEnderecos().isEmpty();
    }

    private static Set<Long> ids(Pessoa pessoa) {
        return pessoa.getEnderecos() == null
                ? Set.of()
                : pessoa.getEnderecos().stream().map(Endereco::getIdEndereco).collect(Collectors.toSet());
    }

    // Mesma chave = mesmo endereço: os demais campos enviados valem como correção para todos os moradores
    private Endereco obterOuCriar(EnderecoDTO dto) {
        return enderecoRepository.findByCepAndNumeroAndComplemento(dto.getCep(), dto.getNumero(), dto.getComplemento())
                .map(endereco -> {
                    endereco.setRua(dto.getRua());
                    endereco.setBairro(dto.getBairro());
                    endereco.setCidade(dto.getCidade());
                    endereco.setEstado(dto.getEstado());
                    return endereco;
                })
                .orElseGet(() -> enderecoMapper.criarEndereco(dto));
    }

    private void vincular(Pessoa pessoa, List<Endereco> enderecos) {
        List<Endereco> anteriores = pessoa.getEnderecos() == null ? List.of() : new ArrayList<>(pessoa.getEnderecos());

        for (Endereco endereco : enderecos) {
            if (!endereco.getMoradores().contains(pessoa)) {
                endereco.getMoradores().add(pessoa);
            }
        }
        enderecoRepository.saveAll(enderecos);

        for (Endereco anterior : anteriores) {
            if (enderecos.stream().anyMatch(endereco -> Objects.equals(endereco.getIdEndereco(), anterior.getIdEndereco()))) {
                continue;
            }
            anterior.getMoradores().remove(pessoa);
            if (anterior.getMoradores().isEmpty()) {
                enderecoRepository.delete(anterior);
            } else {
                enderecoRepository.save(anterior);
            }
        }
        pessoa.setEnderecos(enderecos);
    }
}
