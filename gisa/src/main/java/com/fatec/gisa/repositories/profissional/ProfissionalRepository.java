package com.fatec.gisa.repositories.profissional;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.profissional.Profissional;

@Repository
public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    Optional<Profissional> findByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdCadastroNot(String email, Long idCadastro);

    List<Profissional> findByCargo_IdCargoOrderByNomeAsc(Long idCargo);

    boolean existsByCargo_IdCargo(Long idCargo);

    @Query("""
            select p from Profissional p
            where p.cpf like concat('%', :cpf, '%')
              and lower(p.nome) like lower(concat('%', :nome, '%'))
            """)
    Page<Profissional> buscarPorCpfENome(@Param("cpf") String cpf, @Param("nome") String nome, Pageable pageable);
}
