package com.fatec.gisa.repositories.especialista;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.especialista.Especialista;

@Repository
public interface EspecialistaRepository extends JpaRepository<Especialista, Long> {

    Optional<Especialista> findByRegistroConselho(String registroConselho);
    boolean existsByRegistroConselho(String registroConselho);
    Optional<Especialista> findByCpf(String cpf);

    // As consultas abaixo incluem especialistas PJ (subclasse) e cadastros inativos

    // Especialistas de cada especialidade, em uma única consulta: [idEspecialidade, quantidade]
    @Query("""
            select especialidade.idEspecialidade, count(e) from Especialista e join e.especialidades especialidade
            group by especialidade.idEspecialidade
            """)
    List<Object[]> contarPorEspecialidade();

    @Query("""
            select e from Especialista e join e.especialidades especialidade
            where especialidade.idEspecialidade = :idEspecialidade
            order by e.nome
            """)
    List<Especialista> buscarPorEspecialidade(@Param("idEspecialidade") Long idEspecialidade);

    @Query("""
            select count(e) > 0 from Especialista e join e.especialidades especialidade
            where especialidade.idEspecialidade = :idEspecialidade
            """)
    boolean existePorEspecialidade(@Param("idEspecialidade") Long idEspecialidade);
}
