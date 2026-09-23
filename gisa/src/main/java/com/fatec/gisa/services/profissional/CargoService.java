package com.fatec.gisa.services.profissional;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.profissional.request.CargoRequestDTO;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Cbo;
import com.fatec.gisa.repositories.profissional.CBORepository;
import com.fatec.gisa.repositories.profissional.CargoRepository;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CargoService {

    private final CargoRepository cargoRepository;
    private final CBORepository cboRepository;

    @Transactional
    public Cargo criar(CargoRequestDTO dto) {
        if (cargoRepository.findByNome(dto.getNome()).isPresent()) {
            throw new IllegalArgumentException("Já existe um cargo com esse nome.");
        }

        Cbo cbo = cboRepository.findByCodigoCBO(dto.getCodigoCBO())
                .orElseThrow(() -> new IllegalArgumentException(
                        "CBO não encontrado com código: " + dto.getCodigoCBO()));

        Cargo cargo = new Cargo();
        cargo.setNome(dto.getNome());
        cargo.setCbo(cbo);

        return cargoRepository.save(cargo);
    }

    @Transactional
    public List<Cargo> listarTodos() {
        return cargoRepository.findAll();
    }
}
