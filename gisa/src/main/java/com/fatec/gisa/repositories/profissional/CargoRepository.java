package com.fatec.gisa.repositories.profissional;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.profissional.Cargo;

@Repository
public interface CargoRepository extends JpaRepository<Cargo, Long> {

    Optional<Cargo> findByNome(String nome);

    List<Cargo> findByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdCargoNot(String nome, Long idCargo);

    List<Cargo> findByCbo_CodigoCBOOrderByNomeAsc(String codigoCBO);

    boolean existsByCbo_CodigoCBO(String codigoCBO);
}
