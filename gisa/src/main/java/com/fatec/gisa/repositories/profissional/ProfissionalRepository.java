package com.fatec.gisa.repositories.profissional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.profissional.Profissional;
import com.fatec.gisa.enums.StatusCadastro;

@Repository
public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    Optional<Profissional> findByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdCadastroNot(String email, Long idCadastro);

    List<Profissional> findByCargo_IdCargoOrderByNomeAsc(Long idCargo);

    boolean existsByCargo_IdCargo(Long idCargo);

    // type(p) é a classe exata do cadastro (a herança é JOINED): Profissional, Especialista ou
    // EspecialistaPJ. Os três indicadores dizem quais tipos entram; sem filtro, todos vêm true.
    @Query("""
            select p from Profissional p
            where p.cpf like concat('%', :cpf, '%')
              and lower(p.nome) like lower(concat('%', :nome, '%'))
              and p.statusCadastro in :status
              and ((:profissional = true and type(p) = Profissional)
                or (:especialista = true and type(p) = Especialista)
                or (:especialistaPJ = true and type(p) = EspecialistaPJ))
            """)
    Page<Profissional> buscar(
            @Param("cpf") String cpf,
            @Param("nome") String nome,
            @Param("status") Collection<StatusCadastro> status,
            @Param("profissional") boolean profissional,
            @Param("especialista") boolean especialista,
            @Param("especialistaPJ") boolean especialistaPJ,
            Pageable pageable);
}
