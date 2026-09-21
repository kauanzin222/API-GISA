package com.fatec.gisa.repositories.profissional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.profissional.Cargo;

@Repository
public interface CargoRepository extends JpaRepository<Cargo, Long> {
}
