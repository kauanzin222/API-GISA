package com.fatec.gisa.repositories.profissional;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.profissional.Cbo;

@Repository
public interface CBORepository extends JpaRepository<Cbo, String> {

    Optional<Cbo> findByCodigoCBO(String codigoCBO);

    @Query("""
            select c from Cbo c
            where c.codigoCBO like concat(:codigo, '%')
              and lower(c.tituloCBO) like lower(concat('%', :titulo, '%'))
            """)
    Page<Cbo> buscarPorCodigoETitulo(@Param("codigo") String codigo, @Param("titulo") String titulo, Pageable pageable);
}
