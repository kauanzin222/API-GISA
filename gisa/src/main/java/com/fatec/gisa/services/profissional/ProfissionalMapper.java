package com.fatec.gisa.services.profissional;

import org.springframework.stereotype.Component;

import com.fatec.gisa.dtos.profissional.request.ProfissionalCadastroRequestDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResumoDTO;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Profissional;
import com.fatec.gisa.services.PessoaMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProfissionalMapper {

    private final PessoaMapper pessoaMapper;

    public void preencherProfissional(Profissional profissional,
            ProfissionalCadastroRequestDTO dto, Cargo cargo) {
        pessoaMapper.preencherPessoa(profissional, dto);
        profissional.setEmail(dto.getEmail());
        profissional.setCargo(cargo);
    }

    public ProfissionalResumoDTO toResumoDTO(Profissional profissional) {
        String registroConselho = (profissional instanceof Especialista especialista)
                ? especialista.getRegistroConselho()
                : null;

        String nomeCargo = (profissional.getCargo() != null)
                ? profissional.getCargo().getNome()
                : null;

        String status = (profissional.getStatusCadastro() != null)
                ? profissional.getStatusCadastro().name()
                : null;

        return new ProfissionalResumoDTO(
                profissional.getIdCadastro(),
                profissional.getNome(),
                nomeCargo,
                registroConselho,
                status);
    }
}
