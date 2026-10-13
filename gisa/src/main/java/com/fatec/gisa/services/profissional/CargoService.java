package com.fatec.gisa.services.profissional;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.PessoaVinculadaDTO;
import com.fatec.gisa.dtos.profissional.request.CargoPatchRequestDTO;
import com.fatec.gisa.dtos.profissional.request.CargoRequestDTO;
import com.fatec.gisa.dtos.profissional.response.CargoDetalheDTO;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Cbo;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.repositories.profissional.CBORepository;
import com.fatec.gisa.repositories.profissional.CargoRepository;
import com.fatec.gisa.repositories.profissional.ProfissionalRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CargoService {

    private final CargoRepository cargoRepository;
    private final CBORepository cboRepository;
    private final ProfissionalRepository profissionalRepository;

    @Transactional
    public Cargo criar(CargoRequestDTO dto) {
        String nome = dto.getNome().trim();
        if (cargoRepository.existsByNomeIgnoreCase(nome)) {
            throw new IllegalArgumentException("Já existe um cargo com esse nome.");
        }

        Cargo cargo = new Cargo();
        cargo.setNome(nome);
        cargo.setCbo(buscarCbo(dto.getCodigoCBO()));

        return cargoRepository.save(cargo);
    }

    @Transactional(readOnly = true)
    public List<Cargo> listarTodos(String nome) {
        return cargoRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc(nome == null ? "" : nome.trim());
    }

    @Transactional(readOnly = true)
    public CargoDetalheDTO buscarDetalhes(Long idCargo) {
        Cargo cargo = buscarCargo(idCargo);

        List<PessoaVinculadaDTO> profissionais = profissionalRepository
                .findByCargo_IdCargoOrderByNomeAsc(idCargo).stream()
                .map(profissional -> new PessoaVinculadaDTO(
                        profissional.getIdCadastro(),
                        profissional.getNome(),
                        profissional.getCpf(),
                        profissional.getStatusCadastro()))
                .toList();

        return new CargoDetalheDTO(
                cargo.getIdCargo(),
                cargo.getNome(),
                cargo.getCbo().getCodigoCBO(),
                cargo.getCbo().getTituloCBO(),
                profissionais);
    }

    @Transactional
    public Cargo atualizar(Long idCargo, CargoPatchRequestDTO dto) {
        Cargo cargo = buscarCargo(idCargo);

        if (dto.hasField("nome")) {
            if (dto.getNome() == null || dto.getNome().isBlank()) {
                throw new IllegalArgumentException("O nome do cargo não pode ser vazio.");
            }
            String nome = dto.getNome().trim();
            if (cargoRepository.existsByNomeIgnoreCaseAndIdCargoNot(nome, idCargo)) {
                throw new IllegalArgumentException("Já existe um cargo com esse nome.");
            }
            cargo.setNome(nome);
        }

        if (dto.hasField("codigoCBO")) {
            if (dto.getCodigoCBO() == null || dto.getCodigoCBO().isBlank()) {
                throw new IllegalArgumentException("O código CBO não pode ser vazio.");
            }
            cargo.setCbo(buscarCbo(dto.getCodigoCBO()));
        }

        return cargoRepository.save(cargo);
    }

    @Transactional
    public void excluir(Long idCargo) {
        Cargo cargo = buscarCargo(idCargo);
        if (profissionalRepository.existsByCargo_IdCargo(idCargo)) {
            throw new IllegalArgumentException(
                    "Não é possível excluir o cargo: há profissionais vinculados a ele.");
        }
        cargoRepository.delete(cargo);
    }

    private Cargo buscarCargo(Long idCargo) {
        return cargoRepository.findById(idCargo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cargo não encontrado com ID: " + idCargo));
    }

    private Cbo buscarCbo(String codigoCBO) {
        return cboRepository.findByCodigoCBO(codigoCBO)
                .orElseThrow(() -> new IllegalArgumentException(
                        "CBO não encontrado com código: " + codigoCBO));
    }
}
