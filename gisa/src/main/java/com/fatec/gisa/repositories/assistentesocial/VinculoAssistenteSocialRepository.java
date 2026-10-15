package com.fatec.gisa.repositories.assistentesocial;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.assistentesocial.VinculoAssistenteSocial;

// Vínculo ativo = dataFim nula
@Repository
public interface VinculoAssistenteSocialRepository extends JpaRepository<VinculoAssistenteSocial, Long> {

    boolean existsByPaciente_IdCadastroAndAssistenteSocial_IdCadastroAndDataFimIsNull(
            Long idPaciente, Long idAssistenteSocial);

    Optional<VinculoAssistenteSocial> findByPaciente_IdCadastroAndAssistenteSocial_IdCadastroAndDataFimIsNull(
            Long idPaciente, Long idAssistenteSocial);

    // Pacientes acompanhados pelo assistente social, ordenados pelo nome do paciente
    @Query("""
            select v from VinculoAssistenteSocial v join fetch v.paciente p
            where v.assistenteSocial.idCadastro = :idAssistenteSocial and v.dataFim is null
            order by p.nome
            """)
    List<VinculoAssistenteSocial> buscarAtivosDoAssistenteSocial(@Param("idAssistenteSocial") Long idAssistenteSocial);

    // Assistentes sociais que acompanham o paciente, ordenados pelo nome
    @Query("""
            select v from VinculoAssistenteSocial v join fetch v.assistenteSocial a
            where v.paciente.idCadastro = :idPaciente and v.dataFim is null
            order by a.nome
            """)
    List<VinculoAssistenteSocial> buscarAtivosDoPaciente(@Param("idPaciente") Long idPaciente);

}
