package com.fatec.gisa.services.terapia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import com.fatec.gisa.JornadasDeTeste;
import com.fatec.gisa.dtos.terapia.request.TerapiaCadastroRequestDTO;
import com.fatec.gisa.dtos.terapia.request.TerapiaPatchRequestDTO;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Cbo;
import com.fatec.gisa.enums.EstadoCivil;
import com.fatec.gisa.enums.StatusPaciente;
import com.fatec.gisa.exceptions.ConflitoHorarioException;
import com.fatec.gisa.repositories.terapia.TerapiaRepository;

import jakarta.persistence.EntityManager;

// Sem @Transactional: cada cadastro roda e confirma na própria transação, como em
// requisições HTTP simultâneas. LOCK_TIMEOUT alto para as transações esperarem a trava.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:terapia-concorrencia;MODE=PostgreSQL;LOCK_TIMEOUT=10000",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef"
})
class TerapiaConcorrenciaTest {

    private static final int CADASTROS_SIMULTANEOS = 8;

    @Autowired
    private TerapiaService terapiaService;

    @Autowired
    private TerapiaRepository terapiaRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void apenasUmCadastroSimultaneoNoMesmoHorarioEGravado() throws Exception {
        // Mesmo especialista, pacientes diferentes, mesmo horário
        List<Long> ids = transactionTemplate.execute(status -> {
            Cbo cbo = new Cbo("999999", "Teste");
            entityManager.persist(cbo);
            Cargo cargo = new Cargo(null, "Terapeuta", cbo);
            entityManager.persist(cargo);

            List<Long> criados = new ArrayList<>();
            criados.add(especialista(cargo).getIdCadastro());
            for (int i = 0; i < CADASTROS_SIMULTANEOS; i++) {
                criados.add(paciente(i).getIdCadastro());
            }
            return criados;
        });
        Long idEspecialista = ids.get(0);
        // Os testes desta classe compartilham o banco: conta só o que este teste gravou
        long terapiasAntes = terapiaRepository.count();

        ExecutorService executor = Executors.newFixedThreadPool(CADASTROS_SIMULTANEOS);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (int i = 0; i < CADASTROS_SIMULTANEOS; i++) {
            Long idPaciente = ids.get(i + 1);
            resultados.add(executor.submit(() -> {
                largada.await();
                try {
                    terapiaService.cadastrar(new TerapiaCadastroRequestDTO(
                            List.of(idPaciente), List.of(idEspecialista),
                            LocalDate.of(2099, 10, 27), LocalTime.of(9, 0), LocalTime.of(10, 0)));
                    return true;
                } catch (ConflitoHorarioException conflito) {
                    assertThat(conflito).hasMessageStartingWith("Conflito de horário:");
                    return false;
                }
            }));
        }

        largada.countDown();
        int gravados = 0;
        for (Future<Boolean> resultado : resultados) {
            if (resultado.get(30, TimeUnit.SECONDS)) {
                gravados++;
            }
        }
        executor.shutdown();

        assertThat(gravados).isEqualTo(1);
        assertThat(terapiaRepository.count()).isEqualTo(terapiasAntes + 1);
    }

    // Terapias do mesmo especialista em horários diferentes, remarcadas ao mesmo tempo para o mesmo
    // horário: a edição usa a mesma trava do cadastro, então só uma é aceita
    @Test
    void apenasUmaEdicaoSimultaneaParaOMesmoHorarioEGravada() throws Exception {
        List<Long> ids = transactionTemplate.execute(status -> {
            Cbo cbo = new Cbo("888888", "Teste");
            entityManager.persist(cbo);
            Cargo cargo = new Cargo(null, "Terapeuta", cbo);
            entityManager.persist(cargo);
            Especialista especialista = new Especialista();
            preencherPessoa(especialista, "Bruno Alves", "30000000002");
            especialista.setEmail("bruno@email.com");
            especialista.setCargo(cargo);
            especialista.setRegistroConselho("CRP-2");
            especialista.setEspecialidades(new ArrayList<>());
            especialista.setJornadaTrabalho(JornadasDeTeste.diaInteiroTodosOsDias(especialista));
            entityManager.persist(especialista);

            List<Long> pacientes = new ArrayList<>();
            for (int i = 0; i < CADASTROS_SIMULTANEOS; i++) {
                Paciente paciente = new Paciente();
                preencherPessoa(paciente, "Paciente E" + i, "4100000000" + i);
                paciente.setStatusPaciente(StatusPaciente.MATRICULADO);
                paciente.setDataCadastro(LocalDate.of(2026, 1, 1));
                entityManager.persist(paciente);
                pacientes.add(paciente.getIdCadastro());
            }
            List<Long> criados = new ArrayList<>();
            criados.add(especialista.getIdCadastro());
            criados.addAll(pacientes);
            return criados;
        });
        Long idEspecialista = ids.get(0);
        List<Long> terapias = new ArrayList<>();
        for (int i = 0; i < CADASTROS_SIMULTANEOS; i++) {
            terapias.add(terapiaService.cadastrar(new TerapiaCadastroRequestDTO(
                    List.of(ids.get(i + 1)), List.of(idEspecialista),
                    LocalDate.of(2099, 11, 3), LocalTime.of(8 + i, 0), LocalTime.of(8 + i, 30))).getIdTerapia());
        }

        ExecutorService executor = Executors.newFixedThreadPool(CADASTROS_SIMULTANEOS);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (Long idTerapia : terapias) {
            resultados.add(executor.submit(() -> {
                largada.await();
                TerapiaPatchRequestDTO dto = new TerapiaPatchRequestDTO();
                dto.setHoraInicio(LocalTime.of(18, 0));
                dto.setHoraTermino(LocalTime.of(18, 50));
                try {
                    terapiaService.atualizar(idTerapia, dto);
                    return true;
                } catch (ConflitoHorarioException conflito) {
                    assertThat(conflito).hasMessageStartingWith("Conflito de horário:");
                    return false;
                }
            }));
        }

        largada.countDown();
        int gravados = 0;
        for (Future<Boolean> resultado : resultados) {
            if (resultado.get(30, TimeUnit.SECONDS)) {
                gravados++;
            }
        }
        executor.shutdown();

        assertThat(gravados).isEqualTo(1);
    }

    private Especialista especialista(Cargo cargo) {
        Especialista especialista = new Especialista();
        preencherPessoa(especialista, "Ana Souza", "30000000001");
        especialista.setEmail("ana@email.com");
        especialista.setCargo(cargo);
        especialista.setRegistroConselho("CRP-1");
        especialista.setEspecialidades(new ArrayList<>());
        especialista.setJornadaTrabalho(JornadasDeTeste.diaInteiroTodosOsDias(especialista));
        entityManager.persist(especialista);
        return especialista;
    }

    private Paciente paciente(int indice) {
        Paciente paciente = new Paciente();
        preencherPessoa(paciente, "Paciente " + indice, "4000000000" + indice);
        paciente.setStatusPaciente(StatusPaciente.MATRICULADO);
        paciente.setDataCadastro(LocalDate.of(2026, 1, 1));
        entityManager.persist(paciente);
        return paciente;
    }

    private void preencherPessoa(Pessoa pessoa, String nome, String cpf) {
        pessoa.setNome(nome);
        pessoa.setCpf(cpf);
        pessoa.setDataNascimento(LocalDate.of(1990, 1, 1));
        pessoa.setSexo('F');
        pessoa.setEstadoCivil(EstadoCivil.SOLTEIRO);
    }
}
