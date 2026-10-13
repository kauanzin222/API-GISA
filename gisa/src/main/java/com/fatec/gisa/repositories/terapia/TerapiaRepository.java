package com.fatec.gisa.repositories.terapia;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.terapia.Terapia;
import com.fatec.gisa.enums.StatusTerapia;

@Repository
public interface TerapiaRepository extends JpaRepository<Terapia, Long> {

    Page<Terapia> findByStatusIn(Collection<StatusTerapia> status, Pageable pageable);

    Page<Terapia> findByEspecialistas_IdCadastro(Long idEspecialista, Pageable pageable);

    Page<Terapia> findByEspecialistas_IdCadastroAndStatusIn(
            Long idEspecialista, Collection<StatusTerapia> status, Pageable pageable);

    // Trava as linhas das pessoas envolvidas até o fim da transação, para que dois
    // cadastros simultâneos com alguém em comum não passem juntos pela verificação de
    // conflito. A ordenação por ID evita deadlock entre cadastros concorrentes.
    @Query(value = """
            select "IDCadastro" from "Pessoa"
            where "IDCadastro" in (:ids)
            order by "IDCadastro"
            for update
            """, nativeQuery = true)
    List<Long> bloquearPessoas(@Param("ids") Collection<Long> ids);

    // Terapias não canceladas dos especialistas que se sobrepõem ao horário
    @Query("""
            select new com.fatec.gisa.repositories.terapia.OcupacaoHorario(
                e.idCadastro, e.nome, t.idTerapia, t.dataHoraInicio, t.dataHoraFim)
            from Terapia t join t.especialistas e
            where e.idCadastro in :ids
              and t.status <> :cancelada
              and t.dataHoraInicio < :fim
              and t.dataHoraFim > :inicio
            order by e.nome, t.dataHoraInicio
            """)
    List<OcupacaoHorario> buscarOcupacoesDeEspecialistas(
            @Param("ids") Collection<Long> ids,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("cancelada") StatusTerapia cancelada);

    // Terapias não canceladas dos pacientes que se sobrepõem ao horário
    @Query("""
            select new com.fatec.gisa.repositories.terapia.OcupacaoHorario(
                p.paciente.idCadastro, p.paciente.nome, t.idTerapia, t.dataHoraInicio, t.dataHoraFim)
            from Terapia t join t.presencaPacientes p
            where p.paciente.idCadastro in :ids
              and t.status <> :cancelada
              and t.dataHoraInicio < :fim
              and t.dataHoraFim > :inicio
            order by p.paciente.nome, t.dataHoraInicio
            """)
    List<OcupacaoHorario> buscarOcupacoesDePacientes(
            @Param("ids") Collection<Long> ids,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("cancelada") StatusTerapia cancelada);
}
