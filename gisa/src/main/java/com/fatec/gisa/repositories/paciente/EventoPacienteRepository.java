package com.fatec.gisa.repositories.paciente;

import java.time.Instant;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.paciente.EventoPaciente;
import com.fatec.gisa.enums.TipoEventoPaciente;

@Repository
public interface EventoPacienteRepository extends JpaRepository<EventoPaciente, Long> {

    // Sem parâmetros nulos (o PostgreSQL não infere o tipo de "? is null"):
    // o service envia todos os tipos e um período amplo quando não há filtro
    @Query("""
            select e from EventoPaciente e
            where e.paciente.idCadastro = :idPaciente
              and e.tipo in :tipos
              and e.dataHora >= :inicio
              and e.dataHora < :fim
            """)
    Page<EventoPaciente> buscar(
            @Param("idPaciente") Long idPaciente,
            @Param("tipos") Collection<TipoEventoPaciente> tipos,
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim,
            Pageable pageable);
}
