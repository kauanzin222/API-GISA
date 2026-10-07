package com.fatec.gisa.repositories.paciente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.paciente.Escola;

@Repository
public interface EscolaRepository extends JpaRepository<Escola, Long> {
}