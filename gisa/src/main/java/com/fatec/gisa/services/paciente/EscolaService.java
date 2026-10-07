package com.fatec.gisa.services.paciente;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.paciente.request.EscolaCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.response.EscolaResponseDTO;
import com.fatec.gisa.entities.paciente.Escola;
import com.fatec.gisa.repositories.paciente.EscolaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EscolaService {

    private final EscolaRepository escolaRepository;

    @Transactional(readOnly = true)
    public Escola buscarOpcional(Long idEscola) {
        if (idEscola == null) {
            return null;
        }

        return escolaRepository.findById(idEscola)
                .orElseThrow(() -> new IllegalArgumentException("Escola não encontrada com ID: " + idEscola));
    }

    @Transactional(readOnly = true)
    public List<EscolaResponseDTO> buscarSugestoes(String nome) {
        if (nome == null || nome.isBlank()) {
            return List.of();
        }

        return escolaRepository.findTop10ByNomeContainingIgnoreCaseOrderByNomeAsc(nome.trim())
                .stream()
                .map(escola -> new EscolaResponseDTO(
                        escola.getIdEscola(),
                        escola.getNome(),
                        escola.getTipoEscola(),
                        escola.getTelefone()))
                .toList();
    }

    @Transactional
    public EscolaResponseDTO cadastrar(EscolaCadastroRequestDTO dto) {
        Escola escola = new Escola();
        escola.setNome(dto.getNome().trim());
        escola.setTipoEscola(dto.getTipoEscola());
        escola.setTelefone(dto.getTelefone());

        Escola escolaSalva = escolaRepository.save(escola);
        return new EscolaResponseDTO(
                escolaSalva.getIdEscola(),
                escolaSalva.getNome(),
                escolaSalva.getTipoEscola(),
                escolaSalva.getTelefone());
    }
}