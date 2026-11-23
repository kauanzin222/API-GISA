package com.fatec.gisa.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.Endereco;

@Repository
public interface EnderecoRepository extends JpaRepository<Endereco, Long> {

    // Linhas com a chave do endereço (CEP + número + complemento), da mais antiga para a mais nova.
    // Recebe a chave já normalizada (CEP só com dígitos, complemento vazio = null) e tolera linhas
    // antigas com máscara no CEP, espaços ou complemento vazio. Pode haver mais de uma linha com a mesma
    // chave (quem se mudou sem mudar a chave, ver EnderecoService)
    default List<Endereco> buscarPelaChave(String cep, String numero, String complemento) {
        if (cep == null || numero == null) {
            return List.of();
        }
        return complemento == null
                ? buscarPelaChaveSemComplemento(cep, numero)
                : buscarPelaChaveComComplemento(cep, numero, complemento);
    }

    // Duas consultas para não passar parâmetro nulo: no PostgreSQL ele chega sem tipo (bytea)
    @Query("""
            select e from Endereco e
            where replace(replace(trim(e.cep), '-', ''), '.', '') = :cep
              and upper(trim(e.numero)) = upper(:numero)
              and (e.complemento is null or trim(e.complemento) = '')
            order by e.idEndereco
            """)
    List<Endereco> buscarPelaChaveSemComplemento(@Param("cep") String cep, @Param("numero") String numero);

    @Query("""
            select e from Endereco e
            where replace(replace(trim(e.cep), '-', ''), '.', '') = :cep
              and upper(trim(e.numero)) = upper(:numero)
              and upper(trim(e.complemento)) = upper(:complemento)
            order by e.idEndereco
            """)
    List<Endereco> buscarPelaChaveComComplemento(@Param("cep") String cep, @Param("numero") String numero,
            @Param("complemento") String complemento);
}
