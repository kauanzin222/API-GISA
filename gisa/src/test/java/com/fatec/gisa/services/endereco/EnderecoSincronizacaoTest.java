package com.fatec.gisa.services.endereco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.EnderecoDTO;
import com.fatec.gisa.dtos.PessoaCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.PacienteCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.PacientePatchRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ProntuarioCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelVinculoPatchDTO;
import com.fatec.gisa.dtos.paciente.response.EventoPacienteDTO;
import com.fatec.gisa.entities.Endereco;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.entities.paciente.Responsavel;
import com.fatec.gisa.enums.EstadoCivil;
import com.fatec.gisa.enums.GrauParentesco;
import com.fatec.gisa.enums.TipoEventoPaciente;
import com.fatec.gisa.exceptions.CampoInvalidoException;
import com.fatec.gisa.exceptions.EnderecoCompartilhadoException;
import com.fatec.gisa.exceptions.EnderecoCompartilhadoException.EnderecoCompartilhado;
import com.fatec.gisa.services.PessoaMapper;
import com.fatec.gisa.services.paciente.HistoricoPacienteService;
import com.fatec.gisa.services.paciente.PacienteService;

import jakarta.persistence.EntityManager;

// Lista de endereços com ID: sincronização, endereço compartilhado (409) e ordem (SPEC-enderecos-A1)
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:endereco-sincronizacao;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef"
})
@Transactional
class EnderecoSincronizacaoTest {

    private static final EnderecoDTO RUA_A = endereco(null, "18000001", "Rua A", "10");
    private static final EnderecoDTO RUA_B = endereco(null, "18000002", "Rua B", "20");
    private static final EnderecoDTO RUA_C = endereco(null, "18000003", "Rua C", "30");

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private HistoricoPacienteService historicoPacienteService;

    @Autowired
    private PessoaMapper pessoaMapper;

    @Autowired
    private EntityManager entityManager;

    // ---------- Lista, ordem e validação ----------

    @Test
    void respostaTrazOIdEOrdenaPeloId() {
        Paciente paciente = cadastrar("20000000001", RUA_B, RUA_A);

        List<EnderecoDTO> enderecos = pessoaMapper.toPessoaResponseDTO(paciente).enderecos();
        assertThat(enderecos).hasSize(2).allSatisfy(endereco -> assertThat(endereco.getIdEndereco()).isNotNull());
        assertThat(enderecos.get(0).getIdEndereco()).isLessThan(enderecos.get(1).getIdEndereco());
    }

    @Test
    void enderecoRepetidoNaListaERecusado() {
        EnderecoDTO mesmoLugar = endereco(null, " 18000-001 ", "Outra rua", "10");
        mesmoLugar.setComplemento("");

        assertThatThrownBy(() -> cadastrar("20000000001", RUA_A, mesmoLugar))
                .isInstanceOf(CampoInvalidoException.class)
                .hasMessage("Endereço repetido na lista.");
    }

    @Test
    void idDeEnderecoDeOutroCadastroERecusado() {
        Paciente outro = cadastrar("20000000001", RUA_A);
        Paciente paciente = cadastrar("20000000002", RUA_B);

        assertThatThrownBy(() -> alterarEnderecos(paciente, comId(RUA_C, idDe(outro))))
                .isInstanceOf(CampoInvalidoException.class)
                .hasMessage("Endereço não pertence a este cadastro.");
    }

    @Test
    void itemComOMesmoIdEOsMesmosValoresNaoMudaNada() {
        Paciente paciente = cadastrar("20000000001", RUA_A);
        Long id = idDe(paciente);

        alterarEnderecos(paciente, comId(RUA_A, id));

        assertThat(idsDe(paciente)).containsExactly(id);
        assertThat(tipos(paciente)).doesNotContain(TipoEventoPaciente.ENDERECO_ALTERADO);
    }

    // ---------- Alteração ----------

    @Test
    void alterarEnderecoQueSoAPessoaUsaAlteraNoLugar() {
        Paciente paciente = cadastrar("20000000001", RUA_A);
        Long id = idDe(paciente);

        alterarEnderecos(paciente, comId(endereco(null, "18000001", "Rua A Nova", "10"), id));

        assertThat(idsDe(paciente)).containsExactly(id);
        assertThat(paciente.getEnderecos().get(0).getRua()).isEqualTo("Rua A Nova");
    }

    @Test
    void alterarEnderecoCompartilhadoSemDizerParaQuemDevolve409ENaoGrava() {
        Paciente ana = cadastrar("20000000001", RUA_A);
        cadastrar("20000000002", RUA_A);
        Long id = idDe(ana);

        assertThatThrownBy(() -> alterarEnderecos(ana, comId(endereco(null, "18000001", "Rua A Nova", "10"), id)))
                .isInstanceOf(EnderecoCompartilhadoException.class)
                .extracting(excecao -> ((EnderecoCompartilhadoException) excecao).getEnderecos())
                .isEqualTo(List.of(new EnderecoCompartilhado(id, 1)));
        assertThat(entityManager.find(Endereco.class, id).getRua()).isEqualTo("Rua A");
    }

    @Test
    void alterarParaTodosMudaALinhaDeTodos() {
        Paciente ana = cadastrar("20000000001", RUA_A);
        Paciente bruno = cadastrar("20000000002", RUA_A);
        Long id = idDe(ana);

        EnderecoDTO novo = comId(endereco(null, "18000001", "Rua A Nova", "10"), id);
        novo.setAlterarParaTodos(true);
        alterarEnderecos(ana, novo);

        assertThat(idsDe(ana)).containsExactly(id);
        assertThat(idsDe(bruno)).containsExactly(id);
        assertThat(bruno.getEnderecos().get(0).getRua()).isEqualTo("Rua A Nova");
    }

    @Test
    void alterarSoParaMimMoveParaOutraLinhaMesmoComAMesmaChave() {
        Paciente ana = cadastrar("20000000001", RUA_A);
        Paciente bruno = cadastrar("20000000002", RUA_A);
        Long id = idDe(ana);

        EnderecoDTO novo = comId(endereco(null, "18000001", "Rua A Nova", "10"), id);
        novo.setAlterarParaTodos(false);
        alterarEnderecos(ana, novo);

        assertThat(idsDe(ana)).hasSize(1).doesNotContain(id);
        assertThat(ana.getEnderecos().get(0).getRua()).isEqualTo("Rua A Nova");
        assertThat(idsDe(bruno)).containsExactly(id);
        assertThat(bruno.getEnderecos().get(0).getRua()).isEqualTo("Rua A");

        // Duas linhas com a mesma chave: um cadastro novo nessa chave reaproveita a mais antiga, sem erro
        Paciente carla = cadastrar("20000000003", RUA_A);
        assertThat(idsDe(carla)).containsExactly(id);

        // Alterar o bairro de uma delas (chave igual) não junta as duas linhas de novo
        EnderecoDTO outroBairro = comId(endereco(null, "18000001", "Rua A Nova", "10"), idDe(ana));
        outroBairro.setBairro("Jardim");
        alterarEnderecos(ana, outroBairro);
        assertThat(idsDe(ana)).doesNotContain(id);
        assertThat(idsDe(bruno)).containsExactly(id);
    }

    @Test
    void itemSemIdComAMesmaChaveEOutraRuaEmLinhaCompartilhadaDevolve409() {
        cadastrar("20000000001", RUA_A);

        assertThatThrownBy(() -> cadastrar("20000000002", endereco(null, "18000001", "Rua A Corrigida", "10")))
                .isInstanceOf(EnderecoCompartilhadoException.class);
    }

    @Test
    void itemSemIdComAMesmaChaveEOsMesmosValoresReaproveitaALinha() {
        Paciente ana = cadastrar("20000000001", RUA_A);
        Paciente bruno = cadastrar("20000000002", RUA_A);

        assertThat(idsDe(bruno)).isEqualTo(idsDe(ana));
    }

    @Test
    void alteracaoNoLugarQueBateComOutraLinhaJuntaAsDuas() {
        Paciente ana = cadastrar("20000000001", RUA_A);
        Paciente bruno = cadastrar("20000000002", RUA_B);
        Long idRuaA = idDe(ana);

        alterarEnderecos(ana, comId(RUA_B, idRuaA));

        assertThat(idsDe(ana)).isEqualTo(idsDe(bruno));
        entityManager.flush();
        assertThat(entityManager.find(Endereco.class, idRuaA)).isNull();
    }

    // ---------- Remoção ----------

    @Test
    void removerOUltimoMoradorApagaALinha() {
        Paciente paciente = cadastrar("20000000001", RUA_A, RUA_B);
        Long idRuaA = paciente.getEnderecos().stream()
                .filter(endereco -> endereco.getRua().equals("Rua A")).findFirst().orElseThrow().getIdEndereco();
        Long idRuaB = paciente.getEnderecos().stream()
                .filter(endereco -> endereco.getRua().equals("Rua B")).findFirst().orElseThrow().getIdEndereco();

        alterarEnderecos(paciente, comId(RUA_B, idRuaB));

        assertThat(idsDe(paciente)).containsExactly(idRuaB);
        entityManager.flush();
        assertThat(entityManager.find(Endereco.class, idRuaA)).isNull();
    }

    // ---------- Responsáveis que residem com o paciente ----------

    @Test
    void quemResideAcompanhaAAlteracaoSem409() {
        Paciente paciente = cadastrar("20000000001", List.of(RUA_A), responsavel("30000000001", false));
        Responsavel mae = paciente.getResponsaveis().get(0).getResponsavel();
        Long id = idDe(paciente);

        alterarEnderecos(paciente, comId(endereco(null, "18000009", "Rua Nova", "99"), id));

        assertThat(idsDe(paciente)).containsExactly(id);
        assertThat(idsDe(mae)).containsExactly(id);
        assertThat(mae.getEnderecos().get(0).getRua()).isEqualTo("Rua Nova");
    }

    @Test
    void quemFoiDesmarcadoComoResidenteFicaNoEnderecoAntigoSem409() {
        Paciente paciente = cadastrar("20000000001", List.of(RUA_A), responsavel("30000000001", false));
        Responsavel mae = paciente.getResponsaveis().get(0).getResponsavel();
        Long id = idDe(paciente);

        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setEnderecos(List.of(comId(endereco(null, "18000009", "Rua Nova", "99"), id)));
        dto.setResponsaveis(List.of(new ResponsavelVinculoPatchDTO(mae.getIdCadastro(), GrauParentesco.MAE, false)));
        pacienteService.atualizar(paciente.getIdCadastro(), dto);

        assertThat(idsDe(paciente)).hasSize(1).doesNotContain(id);
        assertThat(idsDe(mae)).containsExactly(id);
        assertThat(mae.getEnderecos().get(0).getRua()).isEqualTo("Rua A");
    }

    // ---------- Histórico ----------

    @Test
    void alterarEnderecoRegistraUmEventoSemOEndereco() {
        Paciente paciente = cadastrar("20000000001", RUA_A);

        alterarEnderecos(paciente, comId(endereco(null, "18000001", "Rua A Nova", "10"), idDe(paciente)));

        List<EventoPacienteDTO> eventos = historicoPacienteService.listar(paciente.getIdCadastro(), null, null, null,
                PageRequest.of(0, 50)).getContent();
        assertThat(eventos).filteredOn(evento -> evento.tipo() == TipoEventoPaciente.ENDERECO_ALTERADO)
                .extracting(EventoPacienteDTO::descricao).containsExactly("Endereço alterado.");
        assertThat(eventos).noneMatch(evento -> evento.tipo() == TipoEventoPaciente.DADOS_ALTERADOS);
    }

    // ---------- Apoio ----------

    private Paciente cadastrar(String cpf, EnderecoDTO... enderecos) {
        return cadastrar(cpf, List.of(enderecos), responsavel("3" + cpf.substring(1), true));
    }

    private Paciente cadastrar(String cpf, List<EnderecoDTO> enderecos, ResponsavelCadastroRequestDTO responsavel) {
        PacienteCadastroRequestDTO dto = new PacienteCadastroRequestDTO();
        preencher(dto, "Paciente " + cpf, cpf);
        dto.setEnderecos(enderecos);
        dto.setProntuario(new ProntuarioCadastroRequestDTO());
        dto.setResponsaveis(List.of(responsavel));
        return pacienteService.cadastrar(dto);
    }

    private void alterarEnderecos(Paciente paciente, EnderecoDTO... enderecos) {
        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setEnderecos(List.of(enderecos));
        pacienteService.atualizar(paciente.getIdCadastro(), dto);
    }

    private static ResponsavelCadastroRequestDTO responsavel(String cpf, boolean naoReside) {
        ResponsavelCadastroRequestDTO dto = new ResponsavelCadastroRequestDTO();
        preencher(dto, "Responsável " + cpf, cpf);
        dto.setGrauParentesco(GrauParentesco.MAE);
        dto.setNaoResideComPaciente(naoReside);
        return dto;
    }

    private static void preencher(PessoaCadastroRequestDTO dto, String nome, String cpf) {
        dto.setNome(nome);
        dto.setCpf(cpf);
        dto.setDataNascimento(LocalDate.of(1990, 1, 1));
        dto.setSexo('F');
        dto.setEstadoCivil(EstadoCivil.SOLTEIRO);
    }

    private static EnderecoDTO endereco(Long id, String cep, String rua, String numero) {
        return new EnderecoDTO(id, cep, rua, numero, null, "Centro", "Sorocaba", "SP", null);
    }

    private static EnderecoDTO comId(EnderecoDTO dto, Long id) {
        return new EnderecoDTO(id, dto.getCep(), dto.getRua(), dto.getNumero(), dto.getComplemento(),
                dto.getBairro(), dto.getCidade(), dto.getEstado(), null);
    }

    private static Long idDe(Pessoa pessoa) {
        return idsDe(pessoa).get(0);
    }

    private static List<Long> idsDe(Pessoa pessoa) {
        return pessoa.getEnderecos().stream().map(Endereco::getIdEndereco).sorted().toList();
    }

    private List<TipoEventoPaciente> tipos(Paciente paciente) {
        return historicoPacienteService.listar(paciente.getIdCadastro(), null, null, null, PageRequest.of(0, 50))
                .getContent().stream().map(EventoPacienteDTO::tipo).toList();
    }
}
