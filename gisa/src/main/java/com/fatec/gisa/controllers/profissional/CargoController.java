package com.fatec.gisa.controllers.profissional;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.profissional.request.CargoPatchRequestDTO;
import com.fatec.gisa.dtos.profissional.request.CargoRequestDTO;
import com.fatec.gisa.dtos.profissional.response.CargoDetalheDTO;
import com.fatec.gisa.dtos.profissional.response.CargoResponseDTO;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.services.profissional.CargoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cargos")
public class CargoController {

    private final CargoService cargoService;

    @GetMapping
    public ResponseEntity<List<CargoResponseDTO>> listarTodos(@RequestParam(required = false) String nome) {
        List<CargoResponseDTO> cargos = cargoService.listarTodos(nome)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(cargos);
    }

    @GetMapping("/{idCargo}")
    public ResponseEntity<CargoDetalheDTO> buscarPorId(@PathVariable Long idCargo) {
        return ResponseEntity.ok(cargoService.buscarDetalhes(idCargo));
    }

    @PostMapping
    public ResponseEntity<CargoResponseDTO> criar(@Valid @RequestBody CargoRequestDTO dto) {
        Cargo cargo = cargoService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(cargo));
    }

    @PatchMapping("/{idCargo}")
    public ResponseEntity<CargoDetalheDTO> atualizar(
            @PathVariable Long idCargo,
            @Valid @RequestBody CargoPatchRequestDTO request) {
        cargoService.atualizar(idCargo, request);
        return ResponseEntity.ok(cargoService.buscarDetalhes(idCargo));
    }

    @DeleteMapping("/{idCargo}")
    public ResponseEntity<Void> excluir(@PathVariable Long idCargo) {
        cargoService.excluir(idCargo);
        return ResponseEntity.noContent().build();
    }

    private CargoResponseDTO toResponse(Cargo cargo) {
        return new CargoResponseDTO(
                cargo.getIdCargo(),
                cargo.getNome(),
                cargo.getCbo().getCodigoCBO(),
                cargo.getCbo().getTituloCBO());
    }
}
