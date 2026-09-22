package com.fatec.gisa.services.profissional;

import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.profissional.request.ProfissionalCadastroRequestDTO;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Profissional;
import com.fatec.gisa.repositories.profissional.CargoRepository;
import com.fatec.gisa.repositories.profissional.ProfissionalRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfissionalService {

    private final CargoRepository cargoRepository;
    private final ProfissionalRepository profissionalRepository;

    @Transactional
    public Profissional criarProfissional(ProfissionalCadastroRequestDTO dto) {
        Profissional profissional = new Profissional();

        profissional.setNome(dto.getNome());
        profissional.setCpf(dto.getCpf());
        profissional.setRg(dto.getRg());
        profissional.setDataNascimento(dto.getDataNascimento());
        profissional.setSexo(dto.getSexo());
        profissional.setCelular(dto.getCelular());
        profissional.setNumCNS(dto.getNumCNS());
        profissional.setEstadoCivil(dto.getEstadoCivil());
        profissional.setEmail(dto.getEmail());

        Cargo cargo = cargoRepository.findById(dto.getIdCargo())
                .orElseThrow(() -> new IllegalArgumentException("Cargo não encontrado"));

        profissional.setCargo(cargo);

        return profissionalRepository.save(profissional);
    }
}