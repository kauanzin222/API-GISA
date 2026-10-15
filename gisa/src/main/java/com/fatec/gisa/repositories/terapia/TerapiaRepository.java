package com.fatec.gisa.repositories.terapia;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.terapia.Terapia;
import com.fatec.gisa.enums.StatusPresenca;
import com.fatec.gisa.enums.StatusTerapia;

import jakarta.persistence.LockModeType;

@Repository
public interface TerapiaRepository extends JpaRepository<Terapia, Long> {

    // Filtros da listagem; idPaciente/idEspecialista = 0 não restringem (ver FiltroTerapiaDTO).
    // A participação é verificada com exists, e não com join, para não repetir a terapia na página.
    // O dia da terapia é o de dataHoraInicio (início e término caem no mesmo dia).
    @Query("""
            select t from Terapia t
            where t.status in :status
              and t.dataHoraInicio >= :inicio
              and t.dataHoraInicio < :fim
              and (:idPaciente = 0 or exists (
                      select 1 from PresencaPaciente p
                      where p.terapia = t and p.paciente.idCadastro = :idPaciente))
              and (:idEspecialista = 0 or exists (
                      select 1 from Terapia mesma join mesma.especialistas e
                      where mesma = t and e.idCadastro = :idEspecialista))
            """)
    Page<Terapia> buscar(
            @Param("status") Collection<StatusTerapia> status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("idPaciente") Long idPaciente,
            @Param("idEspecialista") Long idEspecialista,
            Pageable pageable);

    // Mudança de status e edição: trava a terapia até o fim da transação
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    // Terapia inexistente também responde false
    @Query("""
            select count(t) > 0 from Terapia t join t.especialistas e
            where t.idTerapia = :idTerapia and e.cpf = :cpf
            """)
    boolean especialistaParticipa(@Param("idTerapia") Long idTerapia, @Param("cpf") String cpf);

    @Query("select t from Terapia t where t.idTerapia = :idTerapia")
    Optional<Terapia> buscarParaAlterar(@Param("idTerapia") Long idTerapia);

    // Alteração de jornada: terapias do especialista com o status pedido (AGENDADA) que começam
    // depois de "agora", na ordem da data
    @Query("""
            select t from Terapia t join t.especialistas e
            where e.idCadastro = :idEspecialista
              and t.status = :status
              and t.dataHoraInicio > :agora
            order by t.dataHoraInicio, t.idTerapia
            """)
    List<Terapia> buscarFuturasDoEspecialista(
            @Param("idEspecialista") Long idEspecialista,
            @Param("status") StatusTerapia status,
            @Param("agora") LocalDateTime agora);

    // Inativação do paciente: terapias com o status pedido (AGENDADA) que começam depois de "agora"
    // e de que o paciente participa. Trava as terapias até o fim da transação, na ordem do ID
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t from Terapia t
            where t.status = :status
              and t.dataHoraInicio > :agora
              and exists (select 1 from PresencaPaciente p
                          where p.terapia = t and p.paciente.idCadastro = :idPaciente)
            order by t.idTerapia
            """)
    List<Terapia> travarFuturasDoPaciente(
            @Param("idPaciente") Long idPaciente,
            @Param("status") StatusTerapia status,
            @Param("agora") LocalDateTime agora);

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

    // Terapias que ocupam o horário dos especialistas: não canceladas, diferentes de idTerapiaIgnorada
    // (a própria terapia, ao revalidar) e com ao menos um paciente sem falta justificada; se todos
    // justificaram a falta, o horário do especialista está liberado
    @Query("""
            select new com.fatec.gisa.repositories.terapia.OcupacaoHorario(
                e.idCadastro, e.nome, t.idTerapia, t.dataHoraInicio, t.dataHoraFim)
            from Terapia t join t.especialistas e
            where e.idCadastro in :ids
              and t.status <> :cancelada
              and t.idTerapia <> :idTerapiaIgnorada
              and exists (select 1 from PresencaPaciente outra
                          where outra.terapia = t and outra.presenca <> :justificada)
              and t.dataHoraInicio < :fim
              and t.dataHoraFim > :inicio
            order by e.nome, t.dataHoraInicio
            """)
    List<OcupacaoHorario> buscarOcupacoesDeEspecialistas(
            @Param("ids") Collection<Long> ids,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("cancelada") StatusTerapia cancelada,
            @Param("justificada") StatusPresenca justificada,
            @Param("idTerapiaIgnorada") Long idTerapiaIgnorada);

    // Terapias que ocupam o horário dos pacientes: não canceladas, diferentes de idTerapiaIgnorada e
    // em que o paciente não justificou a falta (falta justificada libera o horário dele)
    @Query("""
            select new com.fatec.gisa.repositories.terapia.OcupacaoHorario(
                p.paciente.idCadastro, p.paciente.nome, t.idTerapia, t.dataHoraInicio, t.dataHoraFim)
            from Terapia t join t.presencaPacientes p
            where p.paciente.idCadastro in :ids
              and t.status <> :cancelada
              and t.idTerapia <> :idTerapiaIgnorada
              and p.presenca <> :justificada
              and t.dataHoraInicio < :fim
              and t.dataHoraFim > :inicio
            order by p.paciente.nome, t.dataHoraInicio
            """)
    List<OcupacaoHorario> buscarOcupacoesDePacientes(
            @Param("ids") Collection<Long> ids,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("cancelada") StatusTerapia cancelada,
            @Param("justificada") StatusPresenca justificada,
            @Param("idTerapiaIgnorada") Long idTerapiaIgnorada);
}
