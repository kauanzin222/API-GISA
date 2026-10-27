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
import com.fatec.gisa.entities.Endereco;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.entities.paciente.Responsavel;
import com.fatec.gisa.entities.paciente.VinculoResponsavel;
import com.fatec.gisa.enums.EstadoCivil;
import com.fatec.gisa.enums.GrauParentesco;
import com.fatec.gisa.enums.TipoEventoPaciente;

import jakarta.persistence.EntityManager;

// Endereço opcional no cadastro, responsáveis que residem com o paciente e endereços compartilhados
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
    private EntityManager entityManager;

    // ---------- Cadastro ----------

    @Test
    void cadastroSemEnderecoComResponsavelQueReside() {
        Paciente paciente = pacienteService.cadastrar(cadastro("20000000001", null,
                responsavelNovo("30000000001", false, null)));

        assertThat(paciente.getEnderecos()).isEmpty();
        VinculoResponsavel vinculo = paciente.getResponsaveis().get(0);
        assertThat(vinculo.isResideComPaciente()).isTrue();
        assertThat(vinculo.getResponsavel().getEnderecos()).isNullOrEmpty();
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
    void responsavelExistenteComOutroEnderecoNaoPodeResidir() {
        pacienteService.cadastrar(cadastro("20000000001", RUA_B, responsavelNovo("30000000001", false, null)));

        assertThatThrownBy(() -> pacienteService.cadastrar(cadastro("20000000002", RUA_A,
                responsavelNovo("30000000001", false, null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tem outro endereço cadastrado");
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
        pacienteService.cadastrar(cadastro("20000000001", null, responsavelNovo("30000000001", false, null)));

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

        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setEnderecos(List.of(RUA_B));
        dto.setResponsaveis(List.of(
                new ResponsavelVinculoPatchDTO(mae.getIdCadastro(), GrauParentesco.MAE, true),
                new ResponsavelVinculoPatchDTO(pai.getIdCadastro(), GrauParentesco.PAI, false)));
        pacienteService.atualizar(paciente.getIdCadastro(), dto);

        assertThat(ids(mae)).isEqualTo(ids(paciente)).doesNotContain(idRuaA);
        assertThat(ids(pai)).containsExactly(idRuaA);
        assertThat(vinculoDe(paciente, pai).isResideComPaciente()).isFalse();
        assertThat(ultimoEvento(paciente).descricao()).contains("endereços", "residência de " + pai.getNome());
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
    void pacienteSemEnderecoPodeContinuarSemEndereco() {
        Paciente paciente = pacienteService.cadastrar(cadastro("20000000001", null,
                responsavelNovo("30000000001", false, null)));

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
    void marcarComoResidenteQuemTemOutroEnderecoERecusado() {
        Paciente paciente = pacienteService.cadastrar(cadastro("20000000001", RUA_A,
                responsavelNovo("30000000001", true, RUA_B)));
        Responsavel mae = responsavelDe(paciente, "30000000001");

        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setResponsaveis(List.of(new ResponsavelVinculoPatchDTO(mae.getIdCadastro(), GrauParentesco.MAE, true)));
        assertThatThrownBy(() -> pacienteService.atualizar(paciente.getIdCadastro(), dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tem outro endereço cadastrado");
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
    void mudarOEnderecoNoCadastroDoResponsavelTiraAResidenciaDosPacientes() {
        Paciente paciente = pacienteService.cadastrar(cadastro("20000000001", RUA_A,
                responsavelNovo("30000000001", false, null)));
        Responsavel mae = responsavelDe(paciente, "30000000001");
        entityManager.flush();
        entityManager.clear();

        ResponsavelPatchRequestDTO dto = new ResponsavelPatchRequestDTO();
        dto.setEnderecos(List.of(RUA_B));
        responsavelService.atualizar(mae.getIdCadastro(), dto);
        entityManager.flush();
        entityManager.clear();

        Paciente atual = entityManager.find(Paciente.class, paciente.getIdCadastro());
        assertThat(atual.getResponsaveis().get(0).isResideComPaciente()).isFalse();
        assertThat(atual.getEnderecos()).extracting(Endereco::getRua).containsExactly("Rua A");
    }

    // ---------- Apoio ----------

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

    private EventoPacienteDTO ultimoEvento(Paciente paciente) {
        return historicoPacienteService.listar(paciente.getIdCadastro(), null, null, null, PageRequest.of(0, 1))
                .getContent().get(0);
    }
}
