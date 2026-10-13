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

	@Query("""
			select p from Paciente p
			where p.cpf like concat('%', :cpf, '%')
			  and lower(p.nome) like lower(concat('%', :nome, '%'))
			""")
	Page<Paciente> buscarPorCpfENome(@Param("cpf") String cpf, @Param("nome") String nome, Pageable pageable);
}