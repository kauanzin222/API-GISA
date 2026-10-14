package com.fatec.gisa.repositories.paciente;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.paciente.Paciente;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

	@EntityGraph(attributePaths = "cids")
	@Query("select distinct p from Paciente p where p.idCadastro in :ids")
	List<Paciente> buscarComCidsPorIds(@Param("ids") Collection<Long> ids);

	// idEscola 0 = qualquer escola (o PostgreSQL não infere o tipo de "? is null"). O left join
	// mantém, sem o filtro, os pacientes sem escola
	@Query("""
			select p from Paciente p left join p.escola escola
			where p.cpf like concat('%', :cpf, '%')
			  and lower(p.nome) like lower(concat('%', :nome, '%'))
			  and (:idEscola = 0 or escola.idEscola = :idEscola)
			""")
	Page<Paciente> buscar(@Param("cpf") String cpf, @Param("nome") String nome,
			@Param("idEscola") Long idEscola, Pageable pageable);

	// Pacientes de cada escola (ativos e inativos), em uma única consulta: [idEscola, quantidade]
	@Query("""
			select p.escola.idEscola, count(p) from Paciente p
			where p.escola.idEscola in :idsEscolas
			group by p.escola.idEscola
			""")
	List<Object[]> contarPorEscola(@Param("idsEscolas") Collection<Long> idsEscolas);

	boolean existsByEscola_IdEscola(Long idEscola);
}
