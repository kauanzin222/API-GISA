package com.fatec.gisa.repositories.paciente;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.paciente.Responsavel;

@Repository
public interface ResponsavelRepository extends JpaRepository<Responsavel, Long> {
    Optional<Responsavel> findByCpf(String cpf);

    @Query("""
            select r from Responsavel r
            where r.cpf like concat('%', :cpf, '%')
              and lower(r.nome) like lower(concat('%', :nome, '%'))
            """)
    Page<Responsavel> buscarPorCpfENome(@Param("cpf") String cpf, @Param("nome") String nome, Pageable pageable);
}
