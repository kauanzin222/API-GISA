package com.fatec.gisa.repositories.paciente;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.paciente.Escola;
import com.fatec.gisa.enums.TipoEscola;

@Repository
public interface EscolaRepository extends JpaRepository<Escola, Long> {

	List<Escola> findTop10ByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

	@Query("""
			select e from Escola e
			where lower(e.nome) like lower(concat('%', :nome, '%'))
			  and e.tipoEscola in :tipos
			""")
	Page<Escola> buscar(@Param("nome") String nome, @Param("tipos") Collection<TipoEscola> tipos,
			Pageable pageable);
}
