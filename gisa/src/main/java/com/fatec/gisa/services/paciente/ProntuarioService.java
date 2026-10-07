package com.fatec.gisa.services.paciente;

import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.paciente.request.ProntuarioCadastroRequestDTO;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.entities.paciente.Prontuario;

@Service
public class ProntuarioService {

    public Prontuario criar(ProntuarioCadastroRequestDTO dto, Paciente paciente) {
        Prontuario prontuario = new Prontuario();
        prontuario.setPaciente(paciente);
        prontuario.setAlergias(dto.getAlergias());
        prontuario.setComorbidade(dto.getComorbidade());
        prontuario.setMobilidade(dto.getMobilidade());
        return prontuario;
    }
}