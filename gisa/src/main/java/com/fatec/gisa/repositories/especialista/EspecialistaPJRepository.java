package com.fatec.gisa.repositories.especialista;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.especialista.EspecialistaPJ;

@Repository
public interface EspecialistaPJRepository extends JpaRepository<EspecialistaPJ, Long> {

    Optional<EspecialistaPJ> findByCnpj(String cnpj);
    boolean existsByCnpj(String cnpj);
}
