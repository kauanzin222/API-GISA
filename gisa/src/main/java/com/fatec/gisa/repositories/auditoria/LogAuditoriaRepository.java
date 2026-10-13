package com.fatec.gisa.repositories.auditoria;

import java.time.Instant;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.auditoria.LogAuditoria;
import com.fatec.gisa.enums.AcaoAuditoria;

@Repository
public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    // Sem parâmetros nulos (o PostgreSQL não infere o tipo de "? is null"):
    // o service envia "" para CPF livre, todas as ações e um período amplo quando não há filtro
    @Query("""
            select l from LogAuditoria l
            where l.cpfUsuario like concat(:cpf, '%')
              and l.acao in :acoes
              and l.dataHora >= :inicio
              and l.dataHora < :fim
            """)
    Page<LogAuditoria> buscar(
            @Param("cpf") String cpf,
            @Param("acoes") Collection<AcaoAuditoria> acoes,
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim,
            Pageable pageable);

    @Query("""
            select l from LogAuditoria l
            where l.acao in :acoes
              and l.dataHora >= :inicio
              and l.dataHora < :fim
            """)
    Page<LogAuditoria> buscarSemFiltroDeCpf(
            @Param("acoes") Collection<AcaoAuditoria> acoes,
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim,
            Pageable pageable);
}
