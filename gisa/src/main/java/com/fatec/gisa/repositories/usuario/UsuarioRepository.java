package com.fatec.gisa.repositories.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.usuario.Usuario;

@Repository 
public interface UsuarioRepository extends JpaRepository<Usuario, Long>{
    Optional<Usuario> findByPessoa_Cpf(String cpf);
    boolean existsByPessoaCpf(String cpf);
}
