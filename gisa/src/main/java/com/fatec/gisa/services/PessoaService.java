package com.fatec.gisa.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.PessoaPatchRequestDTO;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.services.endereco.EnderecoService;

import lombok.RequiredArgsConstructor;

// Campos de pessoa comuns aos PATCH de paciente, responsável, profissional e especialistas
@Service
@RequiredArgsConstructor
public class PessoaService {

    private final EnderecoService enderecoService;

    // Aplica só os campos enviados; os opcionais podem ser limpos com null
    @Transactional
    public void aplicarAlteracoes(Pessoa pessoa, PessoaPatchRequestDTO dto) {
        if (dto.hasField("nome")) {
            if (dto.getNome() == null || dto.getNome().isBlank()) {
                throw new IllegalArgumentException("O nome não pode ser vazio.");
            }
            pessoa.setNome(dto.getNome());
        }

        if (dto.hasField("celular")) {
            pessoa.setCelular(dto.getCelular());
        }

        if (dto.hasField("numCNS")) {
            pessoa.setNumCNS(dto.getNumCNS());
        }

        if (dto.hasField("estadoCivil")) {
            if (dto.getEstadoCivil() == null) {
                throw new IllegalArgumentException("O estado civil não pode ser nulo.");
            }
            pessoa.setEstadoCivil(dto.getEstadoCivil());
        }

        if (dto.hasField("statusCadastro")) {
            if (dto.getStatusCadastro() == null) {
                throw new IllegalArgumentException("O status do cadastro não pode ser nulo.");
            }
            pessoa.setStatusCadastro(dto.getStatusCadastro());
        }

        if (dto.hasField("enderecos")) {
            // Endereço cadastrado não é removido, só alterado (sai apenas se a pessoa for excluída)
            boolean semEndereco = dto.getEnderecos() == null || dto.getEnderecos().isEmpty();
            if (semEndereco && enderecoService.temEndereco(pessoa)) {
                throw new IllegalArgumentException("O endereço não pode ser removido, apenas alterado.");
            }
            enderecoService.substituirEnderecos(pessoa, dto.getEnderecos());
        }
    }
}
