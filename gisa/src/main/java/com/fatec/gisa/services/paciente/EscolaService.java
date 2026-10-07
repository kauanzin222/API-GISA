package com.fatec.gisa.services.paciente;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}