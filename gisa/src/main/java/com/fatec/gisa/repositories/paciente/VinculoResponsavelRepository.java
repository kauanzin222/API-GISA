package com.fatec.gisa.repositories.paciente;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.paciente.VinculoResponsavel;
import com.fatec.gisa.entities.paciente.pk.VinculoResponsavelPK;

@Repository
public interface VinculoResponsavelRepository extends JpaRepository<VinculoResponsavel, VinculoResponsavelPK> {

    @Query("""
            select vinculo from VinculoResponsavel vinculo
            join fetch vinculo.responsavel
            where vinculo.paciente.idCadastro in :pacienteIds
            order by vinculo.paciente.idCadastro, vinculo.responsavel.nome
            """)
    List<VinculoResponsavel> buscarComResponsaveisPorPacienteIds(
            @Param("pacienteIds") Collection<Long> pacienteIds);
}