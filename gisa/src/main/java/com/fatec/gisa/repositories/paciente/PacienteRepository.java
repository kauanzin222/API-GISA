package com.fatec.gisa.repositories.paciente;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fatec.gisa.entities.paciente.Paciente;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

	// Letras acentuadas (minúsculas e maiúsculas) e as correspondentes sem acento, para o translate
	// da busca por nome. As maiúsculas entram porque o lower() do banco pode ignorar letras fora do
	// ASCII, conforme a configuração de idioma do servidor
	String LETRAS_ACENTUADAS = "áàâãäéèêëíìîïóòôõöúùûüçñÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ";
	String LETRAS_SEM_ACENTO = "aaaaaeeeeiiiiooooouuuucnaaaaaeeeeiiiiooooouuuucn";

	@EntityGraph(attributePaths = "cids")
	@Query("select distinct p from Paciente p where p.idCadastro in :ids")
	List<Paciente> buscarComCidsPorIds(@Param("ids") Collection<Long> ids);

	// Busca de pacientes (GET /api/pacientes): IDs ordenados por nome, no máximo "limite". Valores
	// vazios ou 0 não restringem (o PostgreSQL não infere o tipo de "? is null"):
	// - cpf: exato, só dígitos;
	// - nome: trecho já sem acentos e em minúsculas; o nome do banco passa pelo mesmo tratamento;
	// - idEscola: só pacientes desta escola;
	// - idAssistenteSocial: só pacientes com vínculo ativo com este assistente social.
	// Consulta nativa por causa do translate(), que não faz parte do JPQL
	@Query(value = """
			select paciente."IDPaciente"
			from "Paciente" paciente
			join "Pessoa" pessoa on pessoa."IDCadastro" = paciente."IDPaciente"
			where (:cpf = '' or pessoa."CPF" = :cpf)
			  and (:nome = '' or translate(lower(pessoa."Nome"), :acentuadas, :semAcento) like '%' || :nome || '%')
			  and (:idEscola = 0 or paciente."IDEscola" = :idEscola)
			  and (:idAssistenteSocial = 0 or exists (
			          select 1 from "Paciente_AssistenteSocial" vinculo
			          where vinculo."IDPaciente" = paciente."IDPaciente"
			            and vinculo."IDAssistenteSocial" = :idAssistenteSocial
			            and vinculo."DataFim" is null))
			order by pessoa."Nome", paciente."IDPaciente"
			limit :limite
			""", nativeQuery = true)
	List<Long> buscarIds(@Param("cpf") String cpf, @Param("nome") String nome,
			@Param("acentuadas") String acentuadas, @Param("semAcento") String semAcento,
			@Param("idEscola") Long idEscola, @Param("idAssistenteSocial") Long idAssistenteSocial,
			@Param("limite") int limite);

	// Pacientes de cada escola (ativos e inativos), em uma única consulta: [idEscola, quantidade]
	@Query("""
			select p.escola.idEscola, count(p) from Paciente p
			where p.escola.idEscola in :idsEscolas
			group by p.escola.idEscola
			""")
	List<Object[]> contarPorEscola(@Param("idsEscolas") Collection<Long> idsEscolas);

	boolean existsByEscola_IdEscola(Long idEscola);
}
