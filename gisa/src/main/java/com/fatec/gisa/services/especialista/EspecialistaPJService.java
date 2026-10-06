package com.fatec.gisa.services.especialista;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.especialista.request.EspecialistaPJCadastroRequestDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialistaPJResponseDTO;
import com.fatec.gisa.entities.especialista.EspecialistaPJ;
import com.fatec.gisa.repositories.especialista.EspecialistaPJRepository;
import com.fatec.gisa.services.endereco.EnderecoService;
import com.fatec.gisa.services.profissional.ProfissionalMapper;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EspecialistaPJService {

    private final EspecialistaPJRepository especialistaPJRepository;
    private final EspecialistaService especialistaService;
    private final EnderecoService enderecoService;
    private final ProfissionalMapper profissionalMapper;

    @Transactional(readOnly = true)
    public Optional<EspecialistaPJResponseDTO> buscarDetalhes(Long idCadastro) {
        return especialistaPJRepository.findById(idCadastro)
                .map(profissionalMapper::toEspecialistaPJResponseDTO);
    }

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
