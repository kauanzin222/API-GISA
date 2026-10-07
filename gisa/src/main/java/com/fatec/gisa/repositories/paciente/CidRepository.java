package com.fatec.gisa.repositories.paciente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.paciente.Cid;

@Repository
public interface CidRepository extends JpaRepository<Cid, String> {
}