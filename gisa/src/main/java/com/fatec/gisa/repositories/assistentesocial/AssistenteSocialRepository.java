package com.fatec.gisa.repositories.assistentesocial;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.assistentesocial.AssistenteSocial;

@Repository
public interface AssistenteSocialRepository extends JpaRepository<AssistenteSocial, Long> {

    Optional<AssistenteSocial> findByCpf(String cpf);
}
