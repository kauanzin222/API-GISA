package com.fatec.gisa.repositories.usuario;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.usuario.TokenRedefinicaoSenha;

@Repository
public interface TokenRedefinicaoSenhaRepository extends JpaRepository<TokenRedefinicaoSenha, Long> {

    Optional<TokenRedefinicaoSenha> findByTokenHash(String tokenHash);

    // Marca como usados os tokens ainda abertos do usuário: um token novo (ou uma troca de senha)
    // invalida os anteriores
    @Modifying
    @Query("""
            update TokenRedefinicaoSenha t set t.usadoEm = :agora
            where t.idUsuario = :idUsuario and t.usadoEm is null
            """)
    int invalidarAbertos(@Param("idUsuario") Long idUsuario, @Param("agora") Instant agora);
}
