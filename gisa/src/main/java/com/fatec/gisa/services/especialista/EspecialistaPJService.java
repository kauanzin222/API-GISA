package com.fatec.gisa.services.especialista;

import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.especialista.request.EspecialistaPJCadastroRequestDTO;
import com.fatec.gisa.entities.especialista.EspecialistaPJ;
import com.fatec.gisa.repositories.especialista.EspecialistaPJRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EspecialistaPJService {

    private final EspecialistaPJRepository especialistaPJRepository;
    private final EspecialistaService especialistaService;

    @Transactional
    public EspecialistaPJ cadastrar(EspecialistaPJCadastroRequestDTO dto) {
        EspecialistaPJ pj = new EspecialistaPJ();
        especialistaService.preencherDadosEspecialista(pj, dto);

        pj.setCnpj(dto.getCnpj());
        pj.setRazaoSocial(dto.getRazaoSocial());
        pj.setNomeFantasia(dto.getNomeFantasia());
        pj.setInscricaoEstadual(dto.getInscricaoEstadual());

        return especialistaPJRepository.save(pj);
    }
}
