package com.fatec.gisa.services.especialista;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.especialista.request.EspecialistaPJCadastroRequestDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialistaPJPatchRequestDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialistaPJResponseDTO;
import com.fatec.gisa.entities.especialista.EspecialistaPJ;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
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

    @Transactional(readOnly = true)
    public EspecialistaPJ buscarParaConcederAcesso(Long idCadastro) {
        return especialistaPJRepository.findById(idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Especialista PJ não encontrado com ID: " + idCadastro));
    }

    @Transactional
    public EspecialistaPJ cadastrar(EspecialistaPJCadastroRequestDTO dto) {
        if (especialistaPJRepository.existsByCnpj(dto.getCnpj())) {
            throw new IllegalArgumentException("CNPJ já cadastrado.");
        }

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

    @Transactional
    public EspecialistaPJ atualizar(Long idCadastro, EspecialistaPJPatchRequestDTO dto) {
        especialistaService.atualizarParaPJ(idCadastro, dto);
        EspecialistaPJ especialistaPJ = especialistaPJRepository.findById(idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Especialista PJ não encontrado com ID: " + idCadastro));

        if (dto.hasField("razaoSocial")) {
            if (dto.getRazaoSocial() == null || dto.getRazaoSocial().isBlank()) {
                throw new IllegalArgumentException("A razão social não pode ser vazia.");
            }
            especialistaPJ.setRazaoSocial(dto.getRazaoSocial());
        }
        if (dto.hasField("nomeFantasia")) {
            especialistaPJ.setNomeFantasia(dto.getNomeFantasia());
        }
        if (dto.hasField("inscricaoEstadual")) {
            especialistaPJ.setInscricaoEstadual(dto.getInscricaoEstadual());
        }

        return especialistaPJRepository.save(especialistaPJ);
    }

    @Transactional
    public void inativar(Long idCadastro) {
        if (!especialistaPJRepository.existsById(idCadastro)) {
            throw new ResourceNotFoundException(
                    "Especialista PJ não encontrado com ID: " + idCadastro);
        }
        especialistaService.inativarParaPJ(idCadastro);
    }
}
