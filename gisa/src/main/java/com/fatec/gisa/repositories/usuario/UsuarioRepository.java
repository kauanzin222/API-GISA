package com.fatec.gisa.repositories.usuario;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.usuario.Usuario;
import com.fatec.gisa.enums.StatusCadastro;

import jakarta.persistence.LockModeType;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>{
    Optional<Usuario> findByPessoa_Cpf(String cpf);
    boolean existsByPessoa_Cpf(String cpf);
    List<Usuario> findByPerfil_IdPerfilOrderByPessoa_NomeAsc(Long idPerfil);
    // Quantidade de usuários de todos os perfis em uma única consulta: [idPerfil, quantidade]
    @Query("select u.perfil.idPerfil, count(u) from Usuario u group by u.perfil.idPerfil")
    List<Object[]> contarPorPerfil();
    boolean existsByPerfil_IdPerfil(Long idPerfil);

    // Login: o e-mail é comparado sem diferenciar maiúsculas. Devolve lista para o login recusar,
    // sem erro 500, o caso improvável de dois e-mails iguais a menos de maiúsculas
    @Query("select u from Usuario u where lower(u.pessoa.email) = lower(:email)")
    List<Usuario> buscarPorEmail(@Param("email") String email);

    // Usuários com cadastro no status pedido cujo perfil tem a permissão. A trava faz duas
    // retiradas de acesso simultâneas esperarem uma pela outra, para que não removam juntas
    // os dois últimos administradores
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from Usuario u join u.perfil.permissoes permissao
            where permissao.nome = :permissao and u.pessoa.statusCadastro = :status
            """)
    List<Usuario> travarComPermissao(@Param("permissao") String permissao, @Param("status") StatusCadastro status);

    // Como travarComPermissao, para usuários com ao menos uma das permissões. Usa exists (e não
    // join) para não repetir o usuário, já que o PostgreSQL não aceita "distinct" com trava
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from Usuario u
            where u.pessoa.statusCadastro = :status
              and exists (select 1 from Perfil perfil join perfil.permissoes permissao
                          where perfil = u.perfil and permissao.nome in :permissoes)
            """)
    List<Usuario> travarComAlgumaPermissao(@Param("permissoes") Collection<String> permissoes,
            @Param("status") StatusCadastro status);
}
