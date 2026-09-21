package com.fatec.gisa.repositories.especialista;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.especialista.Especialista;

@Repository
public interface EspecialistaRepository extends JpaRepository<Especialista, Long> {

    Optional<Especialista> findByRegistroConselho(String registroConselho);
    boolean existsByRegistroConselho(String registroConselho);
}
