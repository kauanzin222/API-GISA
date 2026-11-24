package com.fatec.gisa.services.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.EnderecoDTO;
import com.fatec.gisa.dtos.paciente.request.PacienteCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.PacientePatchRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ProntuarioCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelPatchRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelVinculoPatchDTO;
import com.fatec.gisa.dtos.paciente.response.EventoPacienteDTO;
import com.fatec.gisa.dtos.paciente.response.ResponsavelVinculoResponseDTO;
import com.fatec.gisa.entities.Endereco;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.entities.paciente.Responsavel;
import com.fatec.gisa.entities.paciente.VinculoResponsavel;
import com.fatec.gisa.enums.EstadoCivil;
import com.fatec.gisa.enums.GrauParentesco;
import com.fatec.gisa.enums.TipoEventoPaciente;
import com.fatec.gisa.exceptions.CampoInvalidoException;
import com.fatec.gisa.exceptions.EnderecoCompartilhadoException;
import com.fatec.gisa.exceptions.EnderecoCompartilhadoException.EnderecoCompartilhado;
import com.fatec.gisa.exceptions.ResidenciaEmOutrosPacientesException;
import com.fatec.gisa.exceptions.ResidenciaEmOutrosPacientesException.ResidenciaEmOutrosPacientes;
import com.fatec.gisa.services.endereco.EnderecoService;

import jakarta.persistence.EntityManager;

// Endereço obrigatório no cadastro do paciente, responsáveis que residem com o paciente e endereços compartilhados
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:endereco-residencia;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef"
})
@Transactional
class EnderecoResidenciaTest {

    private static final EnderecoDTO RUA_A = endereco("18000001", "Rua A", "10");
    private static final EnderecoDTO RUA_B = endereco("18000002", "Rua B", "20");

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private ResponsavelService responsavelService;

    @Autowired
    private HistoricoPacienteService historicoPacienteService;

    @Autowired
    private EnderecoService enderecoService;

    @Autowired
    private EntityManager entityManager;

    // ---------- Cadastro ----------

    @Test
    void cadastroDePacienteSemEnderecoERecusado() {
        assertThatThrownBy(() -> pacienteService.cadastrar(cadastro("20000000001", null,
                responsavelNovo("30000000001", false, null))))
                .isInstanceOf(CampoInvalidoException.class)
                .hasMessage("Informe ao menos um endereço.")
                .extracting("campo").isEqualTo("enderecos");
    }

    @Test
    void responsavelNovoQueResideCompartilhaOEnderecoDoPaciente() {
        Paciente paciente = pacienteService.cadastrar(cadastro("20000000001", RUA_A,
                responsavelNovo("30000000001", false, null)));

        Responsavel responsavel = paciente.getResponsaveis().get(0).getResponsavel();
        assertThat(ids(responsavel)).isEqualTo(ids(paciente)).hasSize(1);
    }

    @Test
    void responsavelNovoQueNaoResidePodeFicarSemEndereco() {
        Paciente paciente = pacienteService.cadastrar(cadastro("20000000001", RUA_A,
                responsavelNovo("30000000001", true, null)));

        VinculoResponsavel vinculo = paciente.getResponsaveis().get(0);
        assertThat(vinculo.isResideComPaciente()).isFalse();
        assertThat(vinculo.getResponsavel().getEnderecos()).isEmpty();
    }

    @Test
    void responsavelExistenteComEnderecoProprioQuePassaAResidirPerdeOEnderecoProprio() {
        Paciente primeiro = pacienteService.cadastrar(cadastro("20000000001", RUA_A,
                responsavelNovo("30000000001", true, RUA_B)));
        Long idRuaB = ids(responsavelDe(primeiro, "30000000001")).get(0);

        Paciente segundo = pacienteService.cadastrar(cadastro("20000000002", RUA_A,
                responsavelNovo("30000000001", false, null)));

        Responsavel responsavel = responsavelDe(segundo, "30000000001");
        assertThat(ids(responsavel)).isEqualTo(ids(segundo)).doesNotContain(idRuaB);
        // O endereço antigo só era dele: sem moradores, é excluído
        entityManager.flush();
        assertThat(entityManager.find(Endereco.class, idRuaB)).isNull();
    }

    @Test
    void responsavelQuePassaAResidirMantemOEnderecoQueDivideComOutroPaciente() {
        Paciente primeiro = pacienteService.cadastrar(cadastro("20000000001", RUA_B,
                responsavelNovo("30000000001", false, null)));

        // Saída (a): continua morando com o primeiro
        ResponsavelCadastroRequestDTO continua = responsavelNovo("30000000001", false, null);
        continua.setContinuaComOutrosPacientes(true);
        Paciente segundo = pacienteService.cadastrar(cadastro("20000000002", RUA_A, continua));

        Responsavel responsavel = responsavelDe(segundo, "30000000001");
        List<Long> esperados = new ArrayList<>(ids(primeiro));
        esperados.addAll(ids(segundo));
        assertThat(ids(responsavel)).containsExactlyInAnyOrderElementsOf(esperados).hasSize(2);
        assertThat(pacienteService.buscarDetalhes(primeiro.getIdCadastro()).responsaveis().get(0)
                .idsEnderecoResidencia()).isEqualTo(ids(primeiro));
    }

    @Test
    void responsavelExistenteComOMesmoEnderecoPodeResidir() {
        pacienteService.cadastrar(cadastro("20000000001", RUA_A, responsavelNovo("30000000001", false, null)));

        Paciente irmao = pacienteService.cadastrar(cadastro("20000000002", RUA_A,
                responsavelNovo("30000000001", false, null)));

        assertThat(irmao.getResponsaveis().get(0).isResideComPaciente()).isTrue();
    }

    @Test
    void responsavelExistenteSemEnderecoRecebeODoPaciente() {
        pacienteService.cadastrar(cadastro("20000000001", RUA_B, responsavelNovo("30000000001", true, null)));

        Paciente outro = pacienteService.cadastrar(cadastro("20000000002", RUA_A,
                responsavelNovo("30000000001", false, null)));

        Responsavel responsavel = outro.getResponsaveis().get(0).getResponsavel();
        assertThat(ids(responsavel)).isEqualTo(ids(outro)).hasSize(1);
    }

    // ---------- PATCH do endereço do paciente ----------

    @Test
    void quemResideVaiJuntoEQuemFoiDesmarcadoFicaNoEnderecoAntigo() {
        Paciente paciente = pacienteService.cadastrar(cadastro("20000000001", RUA_A,
                responsavelNovo("30000000001", false, null),
                responsavelNovo("30000000002", false, null)));
        Responsavel mae = responsavelDe(paciente, "30000000001");
        Responsavel pai = responsavelDe(paciente, "30000000002");
        Long idRuaA = ids(paciente).get(0);

        // O endereço é citado pelo ID (alteração): quem o request marca como não morando nele fica
        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setEnderecos(List.of(comId(RUA_B, idRuaA)));
        dto.setResponsaveis(List.of(
                new ResponsavelVinculoPatchDTO(mae.getIdCadastro(), GrauParentesco.MAE, true),
                new ResponsavelVinculoPatchDTO(pai.getIdCadastro(), GrauParentesco.PAI, false)));
        pacienteService.atualizar(paciente.getIdCadastro(), dto);

        assertThat(ids(mae)).isEqualTo(ids(paciente)).doesNotContain(idRuaA);
        assertThat(ids(pai)).containsExactly(idRuaA);
        assertThat(vinculoDe(paciente, pai).isResideComPaciente()).isFalse();
        assertThat(eventos(paciente)).extracting(EventoPacienteDTO::descricao)
                .contains("Endereço alterado.")
                .anySatisfy(descricao -> assertThat(descricao).startsWith("Dados do cadastro alterados:")
                        .contains("residência de " + pai.getNome()).doesNotContain("endereços"));
    }

    @Test
    void semAListaDeResponsaveisOsResidentesVaoJunto() {
        Paciente paciente = pacienteService.cadastrar(cadastro("20000000001", RUA_A,
                responsavelNovo("30000000001", false, null)));
        Long idRuaA = ids(paciente).get(0);

        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setEnderecos(List.of(RUA_B));
        pacienteService.atualizar(paciente.getIdCadastro(), dto);

        Responsavel mae = responsavelDe(paciente, "30000000001");
        assertThat(ids(mae)).isEqualTo(ids(paciente));
        // Ninguém mais mora no endereço antigo: ele é excluído
        entityManager.flush();
        assertThat(entityManager.find(Endereco.class, idRuaA)).isNull();
    }

    @Test
    void enderecoNaoPodeSerRemovido() {
        Paciente paciente = pacienteService.cadastrar(cadastro("20000000001", RUA_A,
                responsavelNovo("30000000001", false, null)));

        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setEnderecos(List.of());
        assertThatThrownBy(() -> pacienteService.atualizar(paciente.getIdCadastro(), dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O endereço não pode ser removido, apenas alterado.");
    }

    @Test
    void pacienteAntigoSemEnderecoPodeContinuarSemEndereco() {
        Paciente paciente = pacienteService.cadastrar(cadastro("20000000001", RUA_A,
                responsavelNovo("30000000001", true, null)));
        // Cadastro anterior à obrigatoriedade do endereço
        enderecoService.desvincularTodos(paciente);

        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setEnderecos(List.of());
        pacienteService.atualizar(paciente.getIdCadastro(), dto);

        assertThat(paciente.getEnderecos()).isEmpty();
    }

    @Test
    void correcaoComAMesmaChaveValeParaTodosOsMoradores() {
        Paciente paciente = pacienteService.cadastrar(cadastro("20000000001", RUA_A,
                responsavelNovo("30000000001", false, null)));
        Long idRuaA = ids(paciente).get(0);

        EnderecoDTO corrigido = endereco(RUA_A.getCep(), "Rua A Corrigida", RUA_A.getNumero());
        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setEnderecos(List.of(corrigido));
        pacienteService.atualizar(paciente.getIdCadastro(), dto);

        Responsavel mae = responsavelDe(paciente, "30000000001");
        assertThat(ids(paciente)).containsExactly(idRuaA);
        assertThat(mae.getEnderecos().get(0).getRua()).isEqualTo("Rua A Corrigida");
    }

    @Test
    void marcarComoResidenteQuemTemOutroEnderecoSubstituiOEnderecoProprio() {
        Paciente paciente = pacienteService.cadastrar(cadastro("20000000001", RUA_A,
                responsavelNovo("30000000001", true, RUA_B)));
        Responsavel mae = responsavelDe(paciente, "30000000001");
        Long idRuaB = ids(mae).get(0);

        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setResponsaveis(List.of(new ResponsavelVinculoPatchDTO(mae.getIdCadastro(), GrauParentesco.MAE, true)));
        pacienteService.atualizar(paciente.getIdCadastro(), dto);

        assertThat(ids(mae)).isEqualTo(ids(paciente)).doesNotContain(idRuaB);
        assertThat(vinculoDe(paciente, mae).isResideComPaciente()).isTrue();
    }

    // ---------- Irmãos ----------

    @Test
    void irmaoQueFicaNoEnderecoAntigoPassaAConstarComoNaoResidente() {
        Paciente ana = pacienteService.cadastrar(cadastro("20000000001", RUA_A,
                responsavelNovo("30000000001", false, null)));
        Paciente bruno = pacienteService.cadastrar(cadastro("20000000002", RUA_A,
                responsavelNovo("30000000001", false, null)));
        Responsavel maria = responsavelDe(ana, "30000000001");
        entityManager.flush();
        entityManager.clear();

        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setEnderecos(List.of(RUA_B));
        dto.setResponsaveis(List.of(new ResponsavelVinculoPatchDTO(maria.getIdCadastro(), GrauParentesco.MAE, true)));
        pacienteService.atualizar(ana.getIdCadastro(), dto);
        entityManager.flush();
        entityManager.clear();

        Paciente brunoAtual = entityManager.find(Paciente.class, bruno.getIdCadastro());
        assertThat(brunoAtual.getResponsaveis().get(0).isResideComPaciente()).isFalse();
        assertThat(ultimoEvento(brunoAtual)).extracting(EventoPacienteDTO::tipo, EventoPacienteDTO::descricao)
                .containsExactly(TipoEventoPaciente.DADOS_ALTERADOS,
                        "Dados do cadastro alterados: residência de Responsável 30000000001.");
    }

    @Test
    void quemPassaAResidirEMoraComOutroPacienteSemResponderDevolve409ENaoGrava() {
        Paciente primeiro = pacienteService.cadastrar(cadastro("20000000001", RUA_B,
                responsavelNovo("30000000001", false, null)));
        Long idResponsavel = responsavelDe(primeiro, "30000000001").getIdCadastro();
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> pacienteService.cadastrar(cadastro("20000000002", RUA_A,
                responsavelNovo("30000000001", false, null))))
                .isInstanceOf(ResidenciaEmOutrosPacientesException.class)
                .extracting(excecao -> ((ResidenciaEmOutrosPacientesException) excecao).getResidencias())
                .isEqualTo(List.of(new ResidenciaEmOutrosPacientes(idResponsavel, 1)));
    }

    @Test
    void quemSaiuDosOutrosPacientesDeixaDeResidirLaEOEnderecoDelesNaoMuda() {
        Paciente primeiro = pacienteService.cadastrar(cadastro("20000000001", RUA_B,
                responsavelNovo("30000000001", false, null)));
        List<Long> idsDoPrimeiro = ids(primeiro);
        entityManager.flush();
        entityManager.clear();

        ResponsavelCadastroRequestDTO saiu = responsavelNovo("30000000001", false, null);
        saiu.setContinuaComOutrosPacientes(false);
        Paciente segundo = pacienteService.cadastrar(cadastro("20000000002", RUA_A, saiu));
        Responsavel responsavel = responsavelDe(segundo, "30000000001");
        assertThat(ids(responsavel)).isEqualTo(ids(segundo));
        entityManager.flush();
        entityManager.clear();

        Paciente primeiroAtual = entityManager.find(Paciente.class, primeiro.getIdCadastro());
        assertThat(ids(primeiroAtual)).isEqualTo(idsDoPrimeiro);
        assertThat(primeiroAtual.getResponsaveis().get(0).isResideComPaciente()).isFalse();
        assertThat(ultimoEvento(primeiroAtual)).extracting(EventoPacienteDTO::tipo, EventoPacienteDTO::descricao)
                .containsExactly(TipoEventoPaciente.DADOS_ALTERADOS,
                        "Dados do cadastro alterados: residência de Responsável 30000000001.");
    }

    @Test
    void irmaoNoMesmoEnderecoNaoGeraPergunta() {
        pacienteService.cadastrar(cadastro("20000000001", RUA_A, responsavelNovo("30000000001", false, null)));
        entityManager.flush();
        entityManager.clear();

        Paciente irmao = pacienteService.cadastrar(cadastro("20000000002", RUA_A,
                responsavelNovo("30000000001", false, null)));

        assertThat(irmao.getResponsaveis().get(0).isResideComPaciente()).isTrue();
    }

    @Test
    void respostaEIgnoradaQuandoNaoHaOutroPaciente() {
        pacienteService.cadastrar(cadastro("20000000001", RUA_B, responsavelNovo("30000000001", true, null)));
        entityManager.flush();
        entityManager.clear();

        ResponsavelCadastroRequestDTO reside = responsavelNovo("30000000001", false, null);
        reside.setContinuaComOutrosPacientes(false);
        Paciente segundo = pacienteService.cadastrar(cadastro("20000000002", RUA_A, reside));

        assertThat(ids(responsavelDe(segundo, "30000000001"))).isEqualTo(ids(segundo));
    }

    @Test
    void adicionarResponsavelQueMoraComOutroPacienteDevolve409() {
        pacienteService.cadastrar(cadastro("20000000001", RUA_B, responsavelNovo("30000000001", false, null)));
        Paciente segundo = pacienteService.cadastrar(cadastro("20000000002", RUA_A,
                responsavelNovo("30000000002", false, null)));
        entityManager.flush();
        entityManager.clear();

        ResponsavelCadastroRequestDTO dto = responsavelNovo("30000000001", false, null);
        assertThatThrownBy(() -> pacienteService.adicionarResponsavel(segundo.getIdCadastro(), dto))
                .isInstanceOf(ResidenciaEmOutrosPacientesException.class);
        // Na API, o 409 desfaz a transação e o reenvio é outra requisição: descarta o que ficou em memória
        entityManager.clear();

        dto.setContinuaComOutrosPacientes(true);
        Paciente atualizado = pacienteService.adicionarResponsavel(segundo.getIdCadastro(), dto);
        assertThat(ids(responsavelDe(atualizado, "30000000001"))).hasSize(2);
    }

    @Test
    void patchQueMarcaComoResidenteQuemMoraComOutroPacientePergunta() {
        Paciente primeiro = pacienteService.cadastrar(cadastro("20000000001", RUA_B,
                responsavelNovo("30000000001", false, null)));
        Paciente segundo = pacienteService.cadastrar(cadastro("20000000002", RUA_A,
                responsavelNovo("30000000001", true, null)));
        Long id = responsavelDe(segundo, "30000000001").getIdCadastro();
        assertThat(ids(responsavelDe(segundo, "30000000001"))).isEqualTo(ids(primeiro));
        entityManager.flush();
        entityManager.clear();

        PacientePatchRequestDTO semResposta = new PacientePatchRequestDTO();
        semResposta.setResponsaveis(List.of(new ResponsavelVinculoPatchDTO(id, GrauParentesco.MAE, true)));
        assertThatThrownBy(() -> pacienteService.atualizar(segundo.getIdCadastro(), semResposta))
                .isInstanceOf(ResidenciaEmOutrosPacientesException.class);

        PacientePatchRequestDTO saiu = new PacientePatchRequestDTO();
        saiu.setResponsaveis(List.of(new ResponsavelVinculoPatchDTO(id, GrauParentesco.MAE, true, null, false)));
        pacienteService.atualizar(segundo.getIdCadastro(), saiu);
        entityManager.flush();
        entityManager.clear();

        Paciente primeiroAtual = entityManager.find(Paciente.class, primeiro.getIdCadastro());
        assertThat(primeiroAtual.getResponsaveis().get(0).isResideComPaciente()).isFalse();
    }

    // ---------- B1: residência por endereço ----------

    @Test
    void cadaResponsavelFicaLigadoSoAosEnderecosIndicados() {
        Paciente paciente = pacienteService.cadastrar(cadastroComEnderecos("20000000001", List.of(RUA_A, RUA_B),
                responsavelComIndices("30000000001", List.of(0)),
                responsavelComIndices("30000000002", List.of(1)),
                responsavelComIndices("30000000003", List.of())));
        Long idRuaA = idDaRua(paciente, "Rua A");
        Long idRuaB = idDaRua(paciente, "Rua B");

        assertThat(ids(responsavelDe(paciente, "30000000001"))).containsExactly(idRuaA);
        assertThat(ids(responsavelDe(paciente, "30000000002"))).containsExactly(idRuaB);
        assertThat(ids(responsavelDe(paciente, "30000000003"))).isEmpty();

        List<ResponsavelVinculoResponseDTO> responsaveis = pacienteService
                .buscarDetalhes(paciente.getIdCadastro()).responsaveis();
        assertThat(responsaveis).extracting(ResponsavelVinculoResponseDTO::idsEnderecoResidencia)
                .containsExactly(List.of(idRuaA), List.of(idRuaB), List.of());
        assertThat(responsaveis).extracting(ResponsavelVinculoResponseDTO::resideComPaciente)
                .containsExactly(true, true, false);
    }

    @Test
    void requestNoFormatoAntigoFazOResponsavelResidirEmTodosOsEnderecos() {
        Paciente paciente = pacienteService.cadastrar(cadastroComEnderecos("20000000001", List.of(RUA_A, RUA_B),
                responsavelNovo("30000000001", false, null),
                responsavelNovo("30000000002", true, null)));

        assertThat(ids(responsavelDe(paciente, "30000000001"))).isEqualTo(ids(paciente)).hasSize(2);
        assertThat(ids(responsavelDe(paciente, "30000000002"))).isEmpty();
        assertThat(pacienteService.buscarDetalhes(paciente.getIdCadastro()).responsaveis())
                .extracting(ResponsavelVinculoResponseDTO::idsEnderecoResidencia)
                .containsExactly(ids(paciente), List.of());
    }

    @Test
    void alterarOEnderecoOndeSoAMaeMoraAlteraNoLugarEAMaeAcompanha() {
        Paciente paciente = pacienteService.cadastrar(cadastroComEnderecos("20000000001", List.of(RUA_A, RUA_B),
                responsavelComIndices("30000000001", List.of(0)),
                responsavelComIndices("30000000002", List.of(1))));
        Long idRuaA = idDaRua(paciente, "Rua A");
        Long idRuaB = idDaRua(paciente, "Rua B");
        Responsavel mae = responsavelDe(paciente, "30000000001");
        Responsavel pai = responsavelDe(paciente, "30000000002");

        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setEnderecos(List.of(comId(endereco("18000001", "Rua A Nova", "11"), idRuaA), comId(RUA_B, idRuaB)));
        dto.setResponsaveis(List.of(
                new ResponsavelVinculoPatchDTO(mae.getIdCadastro(), GrauParentesco.MAE, null, List.of(0)),
                new ResponsavelVinculoPatchDTO(pai.getIdCadastro(), GrauParentesco.PAI, null, List.of(1))));
        pacienteService.atualizar(paciente.getIdCadastro(), dto);

        assertThat(ids(paciente)).containsExactly(idRuaA, idRuaB);
        assertThat(ids(mae)).containsExactly(idRuaA);
        assertThat(ids(pai)).containsExactly(idRuaB);
        assertThat(mae.getEnderecos().get(0).getRua()).isEqualTo("Rua A Nova");
        assertThat(mae.getEnderecos().get(0).getNumero()).isEqualTo("11");
    }

    @Test
    void maeMarcadaSemResidenciaFicaNoEnderecoAntigoESemConflito() {
        Paciente paciente = pacienteService.cadastrar(cadastroComEnderecos("20000000001", List.of(RUA_A, RUA_B),
                responsavelComIndices("30000000001", List.of(0)),
                responsavelComIndices("30000000002", List.of(1))));
        Long idRuaA = idDaRua(paciente, "Rua A");
        Long idRuaB = idDaRua(paciente, "Rua B");
        Responsavel mae = responsavelDe(paciente, "30000000001");
        Responsavel pai = responsavelDe(paciente, "30000000002");

        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setEnderecos(List.of(comId(endereco("18000001", "Rua A Nova", "11"), idRuaA), comId(RUA_B, idRuaB)));
        dto.setResponsaveis(List.of(
                new ResponsavelVinculoPatchDTO(mae.getIdCadastro(), GrauParentesco.MAE, null, List.of()),
                new ResponsavelVinculoPatchDTO(pai.getIdCadastro(), GrauParentesco.PAI, null, List.of(1))));
        pacienteService.atualizar(paciente.getIdCadastro(), dto);

        assertThat(ids(paciente)).doesNotContain(idRuaA).contains(idRuaB).hasSize(2);
        assertThat(ids(mae)).containsExactly(idRuaA);
        assertThat(mae.getEnderecos().get(0).getNumero()).isEqualTo("10");
        assertThat(vinculoDe(paciente, mae).isResideComPaciente()).isFalse();
        assertThat(vinculoDe(paciente, pai).isResideComPaciente()).isTrue();
    }

    @Test
    void soOsMoradoresDeForaDoPacienteContamNoConflito() {
        Paciente ana = pacienteService.cadastrar(cadastroComEnderecos("20000000001", List.of(RUA_A),
                responsavelComIndices("30000000001", List.of(0))));
        pacienteService.cadastrar(cadastro("20000000002", RUA_A, responsavelNovo("30000000009", true, null)));
        Long idRuaA = idDaRua(ana, "Rua A");
        Responsavel mae = responsavelDe(ana, "30000000001");

        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setEnderecos(List.of(comId(endereco("18000001", "Rua A Nova", "11"), idRuaA)));
        dto.setResponsaveis(List.of(
                new ResponsavelVinculoPatchDTO(mae.getIdCadastro(), GrauParentesco.MAE, null, List.of(0))));

        assertThatThrownBy(() -> pacienteService.atualizar(ana.getIdCadastro(), dto))
                .isInstanceOf(EnderecoCompartilhadoException.class)
                .extracting(excecao -> ((EnderecoCompartilhadoException) excecao).getEnderecos())
                .isEqualTo(List.of(new EnderecoCompartilhado(idRuaA, 1)));
    }

    @Test
    void removerOEnderecoDoPacienteRemoveQuemMoravaNele() {
        Paciente paciente = pacienteService.cadastrar(cadastroComEnderecos("20000000001", List.of(RUA_A, RUA_B),
                responsavelComIndices("30000000001", List.of(0)),
                responsavelComIndices("30000000002", List.of(1))));
        Long idRuaA = idDaRua(paciente, "Rua A");
        Long idRuaB = idDaRua(paciente, "Rua B");
        Responsavel mae = responsavelDe(paciente, "30000000001");
        Responsavel pai = responsavelDe(paciente, "30000000002");

        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setEnderecos(List.of(comId(RUA_A, idRuaA)));
        dto.setResponsaveis(List.of(
                new ResponsavelVinculoPatchDTO(mae.getIdCadastro(), GrauParentesco.MAE, null, List.of(0)),
                new ResponsavelVinculoPatchDTO(pai.getIdCadastro(), GrauParentesco.PAI, null, List.of())));
        pacienteService.atualizar(paciente.getIdCadastro(), dto);

        assertThat(ids(paciente)).containsExactly(idRuaA);
        assertThat(ids(mae)).containsExactly(idRuaA);
        assertThat(ids(pai)).isEmpty();
        assertThat(vinculoDe(paciente, pai).isResideComPaciente()).isFalse();
        entityManager.flush();
        assertThat(entityManager.find(Endereco.class, idRuaB)).isNull();
    }

    @Test
    void enderecoNovoDoPacienteJaLigaQuemFoiMarcadoNele() {
        Paciente paciente = pacienteService.cadastrar(cadastroComEnderecos("20000000001", List.of(RUA_A),
                responsavelComIndices("30000000001", List.of(0)),
                responsavelComIndices("30000000002", List.of(0))));
        Long idRuaA = idDaRua(paciente, "Rua A");
        Responsavel mae = responsavelDe(paciente, "30000000001");
        Responsavel pai = responsavelDe(paciente, "30000000002");

        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setEnderecos(List.of(comId(RUA_A, idRuaA), RUA_B));
        dto.setResponsaveis(List.of(
                new ResponsavelVinculoPatchDTO(mae.getIdCadastro(), GrauParentesco.MAE, null, List.of(0)),
                new ResponsavelVinculoPatchDTO(pai.getIdCadastro(), GrauParentesco.PAI, null, List.of(0, 1))));
        pacienteService.atualizar(paciente.getIdCadastro(), dto);

        assertThat(ids(paciente)).hasSize(2);
        assertThat(ids(mae)).containsExactly(idRuaA);
        assertThat(ids(pai)).isEqualTo(ids(paciente));
    }

    @Test
    void indicesSemOsEnderecosNoRequestSaoRecusados() {
        Paciente paciente = pacienteService.cadastrar(cadastro("20000000001", RUA_A,
                responsavelNovo("30000000001", false, null)));
        Responsavel mae = responsavelDe(paciente, "30000000001");

        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setResponsaveis(List.of(
                new ResponsavelVinculoPatchDTO(mae.getIdCadastro(), GrauParentesco.MAE, null, List.of(0))));
        assertThatThrownBy(() -> pacienteService.atualizar(paciente.getIdCadastro(), dto))
                .isInstanceOf(CampoInvalidoException.class);
    }

    @Test
    void responsavelSemNenhumaInformacaoDeResidenciaFalhaNaValidacaoDoCorpo() {
        ResponsavelCadastroRequestDTO sem = responsavelNovo("30000000001", false, null);
        sem.setNaoResideComPaciente(null);
        jakarta.validation.Validator validador = jakarta.validation.Validation.buildDefaultValidatorFactory()
                .getValidator();

        // (o CPF fictício do teste também é inválido; aqui importa só o campo novo)
        assertThat(validador.validate(sem)).extracting(violacao -> violacao.getPropertyPath().toString())
                .contains("residenciaInformada");

        sem.setIndicesEnderecoResidencia(List.of());
        assertThat(validador.validate(sem)).extracting(violacao -> violacao.getPropertyPath().toString())
                .doesNotContain("residenciaInformada");
    }

    @Test
    void indiceForaDaListaDeEnderecosERecusado() {
        assertThatThrownBy(() -> pacienteService.cadastrar(cadastroComEnderecos("20000000001", List.of(RUA_A),
                responsavelComIndices("30000000001", List.of(1)))))
                .isInstanceOf(CampoInvalidoException.class)
                .extracting("campo").isEqualTo("responsaveis[0].indicesEnderecoResidencia");
    }

    @Test
    void responsavelAdicionadoPorIdsDeEnderecoMoraSoNeles() {
        Paciente paciente = pacienteService.cadastrar(cadastroComEnderecos("20000000001", List.of(RUA_A, RUA_B),
                responsavelComIndices("30000000001", List.of(0, 1))));
        Long idRuaB = idDaRua(paciente, "Rua B");

        ResponsavelCadastroRequestDTO pai = responsavelNovo("30000000002", false, null);
        pai.setNaoResideComPaciente(null);
        pai.setIdsEnderecoResidencia(List.of(idRuaB));
        pacienteService.adicionarResponsavel(paciente.getIdCadastro(), pai);

        assertThat(ids(responsavelDe(paciente, "30000000002"))).containsExactly(idRuaB);
        assertThat(ids(responsavelDe(paciente, "30000000001"))).isEqualTo(ids(paciente));
    }

    @Test
    void idDeEnderecoQueNaoEDoPacienteERecusado() {
        Paciente paciente = pacienteService.cadastrar(cadastro("20000000001", RUA_A,
                responsavelNovo("30000000001", false, null)));
        Paciente outro = pacienteService.cadastrar(cadastro("20000000002", RUA_B,
                responsavelNovo("30000000003", false, null)));

        ResponsavelCadastroRequestDTO pai = responsavelNovo("30000000002", false, null);
        pai.setNaoResideComPaciente(null);
        pai.setIdsEnderecoResidencia(ids(outro));
        assertThatThrownBy(() -> pacienteService.adicionarResponsavel(paciente.getIdCadastro(), pai))
                .isInstanceOf(CampoInvalidoException.class)
                .extracting("campo").isEqualTo("idsEnderecoResidencia");
    }

    @Test
    void naoResidenteComOEnderecoDoPacienteERecusado() {
        assertThatThrownBy(() -> pacienteService.cadastrar(cadastro("20000000001", RUA_A,
                responsavelNovo("30000000001", true, RUA_A))))
                .isInstanceOf(CampoInvalidoException.class)
                .hasMessage("Este é um endereço do paciente. Para morar nele, marque que reside.");
    }

    @Test
    void naoResidenteComMaisDeUmEnderecoProprioERecusado() {
        ResponsavelCadastroRequestDTO mae = responsavelNovo("30000000001", true, null);
        mae.setEnderecos(List.of(RUA_B, endereco("18000003", "Rua C", "30")));
        assertThatThrownBy(() -> pacienteService.cadastrar(cadastro("20000000001", RUA_A, mae)))
                .isInstanceOf(CampoInvalidoException.class)
                .extracting("campo").isEqualTo("enderecos");
    }

    // ---------- B1: endereços pelo cadastro do responsável (RN08) ----------

    @Test
    void responsavelNaoAlteraPeloSeuCadastroUmEnderecoQueETambemDePaciente() {
        Paciente paciente = pacienteService.cadastrar(cadastro("20000000001", RUA_A,
                responsavelNovo("30000000001", false, null)));
        Responsavel mae = responsavelDe(paciente, "30000000001");
        Long idRuaA = ids(paciente).get(0);

        ResponsavelPatchRequestDTO alterado = new ResponsavelPatchRequestDTO();
        alterado.setEnderecos(List.of(comId(endereco("18000001", "Rua A Nova", "10"), idRuaA)));
        assertThatThrownBy(() -> responsavelService.atualizar(mae.getIdCadastro(), alterado))
                .isInstanceOf(CampoInvalidoException.class)
                .hasMessage("Este endereço também é de um paciente. Altere pela edição do paciente.");

        ResponsavelPatchRequestDTO substituido = new ResponsavelPatchRequestDTO();
        substituido.setEnderecos(List.of(RUA_B));
        assertThatThrownBy(() -> responsavelService.atualizar(mae.getIdCadastro(), substituido))
                .isInstanceOf(CampoInvalidoException.class)
                .hasMessage("Este endereço também é de um paciente. Altere pela edição do paciente.");

        ResponsavelPatchRequestDTO igual = new ResponsavelPatchRequestDTO();
        igual.setEnderecos(List.of(comId(RUA_A, idRuaA)));
        responsavelService.atualizar(mae.getIdCadastro(), igual);
        assertThat(ids(mae)).containsExactly(idRuaA);
        assertThat(vinculoDe(paciente, mae).isResideComPaciente()).isTrue();
    }

    @Test
    void responsavelQueNaoResideTrocaOEnderecoProprioPeloSeuCadastroMasSoTemUm() {
        Paciente paciente = pacienteService.cadastrar(cadastro("20000000001", RUA_A,
                responsavelNovo("30000000001", true, RUA_B)));
        Responsavel mae = responsavelDe(paciente, "30000000001");

        ResponsavelPatchRequestDTO troca = new ResponsavelPatchRequestDTO();
        troca.setEnderecos(List.of(endereco("18000003", "Rua C", "30")));
        responsavelService.atualizar(mae.getIdCadastro(), troca);
        assertThat(mae.getEnderecos()).extracting(Endereco::getRua).containsExactly("Rua C");

        ResponsavelPatchRequestDTO dois = new ResponsavelPatchRequestDTO();
        dois.setEnderecos(List.of(RUA_B, endereco("18000004", "Rua D", "40")));
        assertThatThrownBy(() -> responsavelService.atualizar(mae.getIdCadastro(), dois))
                .isInstanceOf(CampoInvalidoException.class);
    }

    // ---------- Apoio ----------

    private static PacienteCadastroRequestDTO cadastroComEnderecos(String cpf, List<EnderecoDTO> enderecos,
            ResponsavelCadastroRequestDTO... responsaveis) {
        PacienteCadastroRequestDTO dto = cadastro(cpf, null, responsaveis);
        dto.setEnderecos(enderecos);
        return dto;
    }

    // Formato novo: sem naoResideComPaciente
    private static ResponsavelCadastroRequestDTO responsavelComIndices(String cpf, List<Integer> indices) {
        ResponsavelCadastroRequestDTO dto = responsavelNovo(cpf, false, null);
        dto.setNaoResideComPaciente(null);
        dto.setIndicesEnderecoResidencia(indices);
        return dto;
    }

    private static EnderecoDTO comId(EnderecoDTO base, Long id) {
        return new EnderecoDTO(id, base.getCep(), base.getRua(), base.getNumero(), base.getComplemento(),
                base.getBairro(), base.getCidade(), base.getEstado(), null);
    }

    private static Long idDaRua(Paciente paciente, String rua) {
        return paciente.getEnderecos().stream()
                .filter(endereco -> endereco.getRua().equals(rua))
                .findFirst()
                .orElseThrow()
                .getIdEndereco();
    }

    private static PacienteCadastroRequestDTO cadastro(String cpf, EnderecoDTO endereco,
            ResponsavelCadastroRequestDTO... responsaveis) {
        PacienteCadastroRequestDTO dto = new PacienteCadastroRequestDTO();
        preencher(dto, "Paciente " + cpf, cpf);
        dto.setEnderecos(endereco == null ? null : List.of(endereco));
        dto.setProntuario(new ProntuarioCadastroRequestDTO());
        dto.setResponsaveis(List.of(responsaveis));
        return dto;
    }

    private static ResponsavelCadastroRequestDTO responsavelNovo(String cpf, boolean naoReside, EnderecoDTO endereco) {
        ResponsavelCadastroRequestDTO dto = new ResponsavelCadastroRequestDTO();
        preencher(dto, "Responsável " + cpf, cpf);
        dto.setGrauParentesco(GrauParentesco.MAE);
        dto.setNaoResideComPaciente(naoReside);
        dto.setEnderecos(endereco == null ? null : List.of(endereco));
        return dto;
    }

    private static void preencher(com.fatec.gisa.dtos.PessoaCadastroRequestDTO dto, String nome, String cpf) {
        dto.setNome(nome);
        dto.setCpf(cpf);
        dto.setDataNascimento(LocalDate.of(1990, 1, 1));
        dto.setSexo('F');
        dto.setEstadoCivil(EstadoCivil.SOLTEIRO);
    }

    private static EnderecoDTO endereco(String cep, String rua, String numero) {
        return new EnderecoDTO(cep, rua, numero, null, "Centro", "Sorocaba", "SP");
    }

    private static List<Long> ids(Pessoa pessoa) {
        return pessoa.getEnderecos() == null
                ? List.of()
                : new ArrayList<>(pessoa.getEnderecos().stream().map(Endereco::getIdEndereco).sorted().toList());
    }

    private static Responsavel responsavelDe(Paciente paciente, String cpf) {
        return paciente.getResponsaveis().stream()
                .map(VinculoResponsavel::getResponsavel)
                .filter(responsavel -> responsavel.getCpf().equals(cpf))
                .findFirst()
                .orElseThrow();
    }

    private static VinculoResponsavel vinculoDe(Paciente paciente, Responsavel responsavel) {
        return paciente.getResponsaveis().stream()
                .filter(vinculo -> vinculo.getResponsavel().equals(responsavel))
                .findFirst()
                .orElseThrow();
    }

    private List<EventoPacienteDTO> eventos(Paciente paciente) {
        return historicoPacienteService.listar(paciente.getIdCadastro(), null, null, null, PageRequest.of(0, 50))
                .getContent();
    }

    private EventoPacienteDTO ultimoEvento(Paciente paciente) {
        return historicoPacienteService.listar(paciente.getIdCadastro(), null, null, null, PageRequest.of(0, 1))
                .getContent().get(0);
    }
}
