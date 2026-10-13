package com.fatec.gisa.services.profissional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.profissional.request.CboPatchRequestDTO;
import com.fatec.gisa.dtos.profissional.request.CboRequestDTO;
import com.fatec.gisa.dtos.profissional.response.CargoResumoDTO;
import com.fatec.gisa.dtos.profissional.response.CboDetalheDTO;
import com.fatec.gisa.dtos.profissional.response.CboResponseDTO;
import com.fatec.gisa.entities.profissional.Cbo;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.repositories.profissional.CBORepository;
import com.fatec.gisa.repositories.profissional.CargoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CboService {

    private final CBORepository cboRepository;
    private final CargoRepository cargoRepository;

    @Transactional(readOnly = true)
    public Page<CboResponseDTO> listarPaginado(String codigo, String titulo, Pageable pageable) {
        return cboRepository.buscarPorCodigoETitulo(
                codigo == null ? "" : codigo.replaceAll("\\D", ""),
                titulo == null ? "" : titulo.trim(),
                pageable)
                .map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public CboDetalheDTO buscarDetalhes(String codigoCBO) {
        Cbo cbo = buscarCbo(codigoCBO);
        return new CboDetalheDTO(
                cbo.getCodigoCBO(),
                cbo.getTituloCBO(),
                cargoRepository.findByCbo_CodigoCBOOrderByNomeAsc(codigoCBO).stream()
                        .map(cargo -> new CargoResumoDTO(cargo.getIdCargo(), cargo.getNome()))
                        .toList());
    }

    @Transactional
    public CboResponseDTO criar(CboRequestDTO dto) {
        if (cboRepository.existsById(dto.codigoCBO())) {
            throw new IllegalArgumentException("CBO já cadastrado.");
        }

        Cbo cbo = new Cbo();
        cbo.setCodigoCBO(dto.codigoCBO());
        cbo.setTituloCBO(dto.tituloCBO().trim());
        return toResponseDTO(cboRepository.save(cbo));
    }

    @Transactional
    public CboResponseDTO atualizar(String codigoCBO, CboPatchRequestDTO dto) {
        Cbo cbo = buscarCbo(codigoCBO);

        if (dto.hasField("tituloCBO")) {
            if (dto.getTituloCBO() == null || dto.getTituloCBO().isBlank()) {
                throw new IllegalArgumentException("O título do CBO não pode ser vazio.");
            }
            cbo.setTituloCBO(dto.getTituloCBO().trim());
        }

        return toResponseDTO(cboRepository.save(cbo));
    }

    @Transactional
    public void excluir(String codigoCBO) {
        Cbo cbo = buscarCbo(codigoCBO);
        if (cargoRepository.existsByCbo_CodigoCBO(codigoCBO)) {
            throw new IllegalArgumentException(
                    "Não é possível excluir o CBO: há cargos vinculados a ele.");
        }
        cboRepository.delete(cbo);
    }

    private Cbo buscarCbo(String codigoCBO) {
        return cboRepository.findByCodigoCBO(codigoCBO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CBO não encontrado com código: " + codigoCBO));
    }

    private CboResponseDTO toResponseDTO(Cbo cbo) {
        return new CboResponseDTO(cbo.getCodigoCBO(), cbo.getTituloCBO());
    }
}
