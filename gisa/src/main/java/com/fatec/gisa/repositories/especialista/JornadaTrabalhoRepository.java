package com.fatec.gisa.repositories.especialista;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.especialista.JornadaTrabalho;

@Repository
public interface JornadaTrabalhoRepository extends JpaRepository<JornadaTrabalho, Long> {
    // Busca todas as jornadas associadas a um especialista específico
    List<JornadaTrabalho> findByEspecialistaIdCadastro(Long idEspecialista);
}
