package com.fatec.gisa.repositories.especialista;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.especialista.Especialidade;

@Repository
public interface EspecialidadeRepository extends JpaRepository<Especialidade, Long> {

    Optional<Especialidade> findByNome(String nome);
    boolean existsByNome(String nome);
}
