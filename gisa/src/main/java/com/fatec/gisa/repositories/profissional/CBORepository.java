package com.fatec.gisa.repositories.profissional;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.profissional.Cbo;

@Repository
public interface CBORepository extends JpaRepository<Cbo, String> {
    
    Optional<Cbo> findByCodigoCBO(String codigoCBO);
}
