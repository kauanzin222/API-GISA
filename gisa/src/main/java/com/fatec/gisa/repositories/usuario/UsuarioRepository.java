package com.fatec.gisa.repositories.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.usuario.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>{
    Optional<Usuario> findByPessoa_Cpf(String cpf);
    boolean existsByPessoa_Cpf(String cpf);
    List<Usuario> findByPerfil_IdPerfilOrderByPessoa_NomeAsc(Long idPerfil);
    // Quantidade de usuários de todos os perfis em uma única consulta: [idPerfil, quantidade]
    @Query("select u.perfil.idPerfil, count(u) from Usuario u group by u.perfil.idPerfil")
    List<Object[]> contarPorPerfil();
    boolean existsByPerfil_IdPerfil(Long idPerfil);
}
