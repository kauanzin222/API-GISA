package com.fatec.gisa.services.especialista;

import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.especialista.request.EspecialistaPJCadastroRequestDTO;
import com.fatec.gisa.entities.especialista.EspecialistaPJ;
import com.fatec.gisa.repositories.especialista.EspecialistaPJRepository;
import com.fatec.gisa.services.endereco.EnderecoService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EspecialistaPJService {

    private final EspecialistaPJRepository especialistaPJRepository;
    private final EspecialistaService especialistaService;
    private final EnderecoService enderecoService;

    @Transactional
    public EspecialistaPJ cadastrar(EspecialistaPJCadastroRequestDTO dto) {
        EspecialistaPJ pj = new EspecialistaPJ();
        especialistaService.preencherDadosEspecialista(pj, dto);

        pj.setCnpj(dto.getCnpj());
        pj.setRazaoSocial(dto.getRazaoSocial());
        pj.setNomeFantasia(dto.getNomeFantasia());
        pj.setInscricaoEstadual(dto.getInscricaoEstadual());

        EspecialistaPJ especialistaPJSaved = especialistaPJRepository.save(pj);
        enderecoService.associarEnderecos(especialistaPJSaved, dto.getEnderecos());
        return especialistaPJSaved;
    }
}
