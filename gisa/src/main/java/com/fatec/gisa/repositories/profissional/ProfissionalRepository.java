package com.fatec.gisa.repositories.profissional;

import java.util.Optional;

import org.hibernate.query.Page;
import org.springframework.boot.data.autoconfigure.web.DataWebProperties.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.profissional.Profissional;

@Repository
public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    Optional<Profissional> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
    Page findAll(Pageable pageable);
}
