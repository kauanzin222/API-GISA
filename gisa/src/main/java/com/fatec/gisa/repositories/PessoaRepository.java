package com.fatec.gisa.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.enums.StatusCadastro;

@Repository 
public interface PessoaRepository extends JpaRepository<Pessoa, Long> {
    Optional<Pessoa> findByCpf(String cpf);
    List<Pessoa> findByCpfIn(Collection<String> cpfs);
    boolean existsByCpf(String cpf);
    List<Pessoa> findByNomeContainingIgnoreCase(String nome);

    // Pessoas com usuário de acesso. Parte de Pessoa (e não de Usuario) para que o sort por
    // nome, cpf e statusCadastro funcione direto. idPerfil 0 = qualquer perfil; os indicadores
    // de tipo seguem ProfissionalRepository.buscar, e "pessoa" é quem não é profissional
    @Query("""
            select p from Pessoa p join p.usuario u
            where p.cpf like concat('%', :cpf, '%')
              and lower(p.nome) like lower(concat('%', :nome, '%'))
              and (:idPerfil = 0 or u.perfil.idPerfil = :idPerfil)
              and p.statusCadastro in :status
              and ((:profissional = true and type(p) = Profissional)
                or (:especialista = true and type(p) = Especialista)
                or (:especialistaPJ = true and type(p) = EspecialistaPJ)
                or (:pessoa = true and type(p) not in (Profissional, Especialista, EspecialistaPJ)))
            """)
    Page<Pessoa> buscarComUsuario(
            @Param("cpf") String cpf,
            @Param("nome") String nome,
            @Param("idPerfil") Long idPerfil,
            @Param("status") Collection<StatusCadastro> status,
            @Param("profissional") boolean profissional,
            @Param("especialista") boolean especialista,
            @Param("especialistaPJ") boolean especialistaPJ,
            @Param("pessoa") boolean pessoa,
            Pageable pageable);
}
