package com.fatec.gisa.services.especialista;

import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.especialista.request.EspecialistaPJCadastroRequestDTO;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.especialista.EspecialistaPJ;
import com.fatec.gisa.repositories.especialista.EspecialistaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EspecialistaPJService {

    private final EspecialistaRepository especialistaRepository;
    private final EspecialistaService especialistaService;

    @Transactional
    public EspecialistaPJ cadastrar(EspecialistaPJCadastroRequestDTO dto) {
        Especialista especialista = especialistaService.cadastrar(dto);

        EspecialistaPJ pj = new EspecialistaPJ();
        pj.setIdCadastro(especialista.getIdCadastro());
        pj.setNome(especialista.getNome());
        pj.setCpf(especialista.getCpf());
        pj.setRg(especialista.getRg());
        pj.setDataNascimento(especialista.getDataNascimento());
        pj.setSexo(especialista.getSexo());
        pj.setCelular(especialista.getCelular());
        pj.setEmail(especialista.getEmail());
        pj.setNumCNS(especialista.getNumCNS());
        pj.setEstadoCivil(especialista.getEstadoCivil());
        pj.setCargo(especialista.getCargo());
        pj.setRegistroConselho(especialista.getRegistroConselho());
        pj.setEspecialidades(especialista.getEspecialidades());
        pj.setJornadaTrabalho(especialista.getJornadaTrabalho());

        pj.setCnpj(dto.getCnpj());
        pj.setRazaoSocial(dto.getRazaoSocial());
        pj.setNomeFantasia(dto.getNomeFantasia());
        pj.setInscricaoEstadual(dto.getInscricaoEstadual());

        return especialistaRepository.save(pj);
    }
}
