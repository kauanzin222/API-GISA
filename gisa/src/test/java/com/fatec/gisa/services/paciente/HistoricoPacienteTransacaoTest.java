package com.fatec.gisa.services.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import com.fatec.gisa.dtos.terapia.request.TerapiaCadastroRequestDTO;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Cbo;
import com.fatec.gisa.enums.EstadoCivil;
import com.fatec.gisa.enums.StatusPaciente;
import com.fatec.gisa.enums.TipoEventoPaciente;
import com.fatec.gisa.repositories.paciente.EventoPacienteRepository;
import com.fatec.gisa.services.terapia.TerapiaService;

import jakarta.persistence.EntityManager;

// Sem @Transactional na classe: cada bloco confirma ou desfaz a própria transação, como uma requisição
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:historico-transacao;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef"
})
class HistoricoPacienteTransacaoTest {

    @Autowired
    private TerapiaService terapiaService;

    @Autowired
    private HistoricoPacienteService historicoPacienteService;

    @Autowired
    private EventoPacienteRepository eventoPacienteRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void eventoSaiJuntoComAOperacaoQuandoElaEDesfeita() {
        List<Long> ids = transactionTemplate.execute(status -> {
            Cbo cbo = new Cbo("777777", "Teste");
            entityManager.persist(cbo);
            Cargo cargo = new Cargo(null, "Terapeuta", cbo);
            entityManager.persist(cargo);

            Especialista especialista = new Especialista();
            preencherPessoa(especialista, "Ana Souza", "10000000001");
            especialista.setEmail("ana@email.com");
            especialista.setCargo(cargo);
            especialista.setRegistroConselho("CRP-1");
            especialista.setEspecialidades(new ArrayList<>());
            especialista.setJornadaTrabalho(new ArrayList<>());
            entityManager.persist(especialista);

            Paciente paciente = new Paciente();
            preencherPessoa(paciente, "Lucas Martins", "20000000001");
            paciente.setStatusPaciente(StatusPaciente.MATRICULADO);
            paciente.setDataCadastro(LocalDate.of(2026, 1, 1));
            entityManager.persist(paciente);
            return List.of(paciente.getIdCadastro(), especialista.getIdCadastro());
        });
        long eventosAntes = eventoPacienteRepository.count();

        // A operação grava a terapia e o evento, e depois a transação é desfeita (como num erro)
        transactionTemplate.executeWithoutResult(status -> {
            terapiaService.cadastrar(new TerapiaCadastroRequestDTO(List.of(ids.get(0)), List.of(ids.get(1)),
                    LocalDate.of(2099, 10, 27), LocalTime.of(9, 0), LocalTime.of(10, 0)));
            assertThat(eventoPacienteRepository.count()).isEqualTo(eventosAntes + 1);
            status.setRollbackOnly();
        });
        assertThat(eventoPacienteRepository.count()).isEqualTo(eventosAntes);

        // Confirmada, o evento fica
        transactionTemplate.executeWithoutResult(status -> terapiaService.cadastrar(new TerapiaCadastroRequestDTO(
                List.of(ids.get(0)), List.of(ids.get(1)),
                LocalDate.of(2099, 10, 27), LocalTime.of(9, 0), LocalTime.of(10, 0))));
        assertThat(eventoPacienteRepository.count()).isEqualTo(eventosAntes + 1);
    }

    @Test
    void naoGravaEventoForaDeUmaTransacao() {
        assertThatThrownBy(() -> historicoPacienteService.registrar(
                new Paciente(), TipoEventoPaciente.CADASTRO, "Paciente cadastrado."))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private void preencherPessoa(Pessoa pessoa, String nome, String cpf) {
        pessoa.setNome(nome);
        pessoa.setCpf(cpf);
        pessoa.setDataNascimento(LocalDate.of(1990, 1, 1));
        pessoa.setSexo('F');
        pessoa.setEstadoCivil(EstadoCivil.SOLTEIRO);
    }
}
