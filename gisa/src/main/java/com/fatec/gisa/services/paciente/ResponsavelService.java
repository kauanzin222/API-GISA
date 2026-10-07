package com.fatec.gisa.services.paciente;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.paciente.request.ResponsavelCadastroRequestDTO;
import com.fatec.gisa.entities.paciente.Responsavel;
import com.fatec.gisa.repositories.PessoaRepository;
import com.fatec.gisa.repositories.paciente.ResponsavelRepository;
import com.fatec.gisa.services.PessoaMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResponsavelService {

    private final ResponsavelRepository responsavelRepository;
    private final PessoaRepository pessoaRepository;
    private final PessoaMapper pessoaMapper;

    @Transactional
    public Responsavel obterOuCriar(ResponsavelCadastroRequestDTO dto) {
        if (dto.getIdResponsavel() != null) {
            Responsavel responsavel = responsavelRepository.findById(dto.getIdResponsavel())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Responsável não encontrado com ID: " + dto.getIdResponsavel()));
            validarCpf(dto, responsavel);
            return responsavel;
        }

        Responsavel responsavelExistente = responsavelRepository.findByCpf(dto.getCpf()).orElse(null);
        if (responsavelExistente != null) {
            return responsavelExistente;
        }

        if (pessoaRepository.existsByCpf(dto.getCpf())) {
            throw new IllegalArgumentException(
                    "O CPF informado pertence a uma pessoa que não está cadastrada como responsável");
        }

        Responsavel responsavel = new Responsavel();
        pessoaMapper.preencherPessoa(responsavel, dto);
        responsavel.setOcupacao(dto.getOcupacao());
        return responsavelRepository.save(responsavel);
    }

    private void validarCpf(ResponsavelCadastroRequestDTO dto, Responsavel responsavel) {
        if (!responsavel.getCpf().equals(dto.getCpf())) {
            throw new IllegalArgumentException("O CPF informado não corresponde ao responsável selecionado");
        }
    }
}