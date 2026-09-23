package com.fatec.gisa.services.profissional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.profissional.request.ProfissionalCadastroRequestDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResumoDTO;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Profissional;
import com.fatec.gisa.repositories.profissional.CargoRepository;
import com.fatec.gisa.repositories.profissional.ProfissionalRepository;
import com.fatec.gisa.services.endereco.EnderecoService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;
    private final CargoRepository cargoRepository;
    private final ProfissionalMapper profissionalMapper;
    private final EnderecoService enderecoService;

    @Transactional
    public Profissional criarProfissional(ProfissionalCadastroRequestDTO dto) {
        Profissional profissional = new Profissional();

        Cargo cargo = cargoRepository.findById(dto.getIdCargo())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cargo nao encontrado com ID: " + dto.getIdCargo()));

        profissionalMapper.preencherProfissional(profissional, dto, cargo);

        Profissional profissionalSalvo = profissionalRepository.save(profissional);
        enderecoService.associarEnderecos(profissionalSalvo, dto.getEnderecos());
        return profissionalSalvo;
    }

    @Transactional(readOnly = true)
    public Page<ProfissionalResumoDTO> listarPaginado(Pageable pageable) {
        return profissionalRepository.findAll(pageable)
                .map(profissionalMapper::toResumoDTO);
    }
}