package com.fatec.gisa.services.profissional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.FiltroPessoaDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResumoDTO;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.especialista.EspecialistaPJ;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Cbo;
import com.fatec.gisa.entities.profissional.Profissional;
import com.fatec.gisa.enums.EstadoCivil;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.enums.TipoCadastro;

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:profissional-listagem;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef"
})
@Transactional
class ProfissionalListagemTest {

    private static final FiltroPessoaDTO SEM_BUSCA = FiltroPessoaDTO.de(null, null);

    @Autowired
    private ProfissionalService profissionalService;

    @Autowired
    private EntityManager entityManager;

    private Cargo cargo;

    @BeforeEach
    void preparar() {
        Cbo cbo = new Cbo("999999", "Teste");
        entityManager.persist(cbo);
        cargo = new Cargo(null, "Terapeuta", cbo);
        entityManager.persist(cargo);

        persistir(new Profissional(), "Ana Recepção", "10000000001", StatusCadastro.ATIVO);
        persistir(new Especialista(), "Bruno Psicólogo", "10000000002", StatusCadastro.ATIVO);
        persistir(new EspecialistaPJ(), "Carla PJ", "10000000003", StatusCadastro.ATIVO);
        persistir(new EspecialistaPJ(), "Davi PJ", "10000000004", StatusCadastro.INATIVO);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void cadaItemTrazOTipo() {
        assertThat(listar(null, null)).extracting(ProfissionalResumoDTO::nome, ProfissionalResumoDTO::tipo)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Ana Recepção", TipoCadastro.PROFISSIONAL),
                        org.assertj.core.groups.Tuple.tuple("Bruno Psicólogo", TipoCadastro.ESPECIALISTA),
                        org.assertj.core.groups.Tuple.tuple("Carla PJ", TipoCadastro.ESPECIALISTA_PJ),
                        org.assertj.core.groups.Tuple.tuple("Davi PJ", TipoCadastro.ESPECIALISTA_PJ));
    }

    @Test
    void filtraPorUmOuMaisTipos() {
        assertThat(listar(List.of(TipoCadastro.PROFISSIONAL), null))
                .extracting(ProfissionalResumoDTO::nome).containsExactly("Ana Recepção");
        // ESPECIALISTA não inclui os PJ (o tipo é exato)
        assertThat(listar(List.of(TipoCadastro.ESPECIALISTA), null))
                .extracting(ProfissionalResumoDTO::nome).containsExactly("Bruno Psicólogo");
        assertThat(listar(List.of(TipoCadastro.ESPECIALISTA, TipoCadastro.ESPECIALISTA_PJ), null))
                .extracting(ProfissionalResumoDTO::nome).containsExactly("Bruno Psicólogo", "Carla PJ", "Davi PJ");
    }

    @Test
    void filtraPorStatusCombinadoComTipoEBusca() {
        assertThat(listar(null, StatusCadastro.INATIVO))
                .extracting(ProfissionalResumoDTO::nome).containsExactly("Davi PJ");
        assertThat(listar(List.of(TipoCadastro.ESPECIALISTA_PJ), StatusCadastro.ATIVO))
                .extracting(ProfissionalResumoDTO::nome).containsExactly("Carla PJ");
        assertThat(profissionalService.listarPaginado(FiltroPessoaDTO.de(null, "pj"),
                        List.of(TipoCadastro.ESPECIALISTA_PJ), StatusCadastro.INATIVO, pagina()).getContent())
                .extracting(ProfissionalResumoDTO::nome).containsExactly("Davi PJ");
    }

    @Test
    void recusaOTipoPessoa() {
        assertThatThrownBy(() -> listar(List.of(TipoCadastro.PESSOA), null))
                .hasMessage("Valor inválido para 'tipo': PESSOA");
    }

    private List<ProfissionalResumoDTO> listar(List<TipoCadastro> tipos, StatusCadastro status) {
        return profissionalService.listarPaginado(SEM_BUSCA, tipos, status, pagina()).getContent();
    }

    private PageRequest pagina() {
        return PageRequest.of(0, 20, Sort.by("nome"));
    }

    private void persistir(Profissional profissional, String nome, String cpf, StatusCadastro status) {
        profissional.setNome(nome);
        profissional.setCpf(cpf);
        profissional.setDataNascimento(LocalDate.of(1990, 1, 1));
        profissional.setSexo('F');
        profissional.setEstadoCivil(EstadoCivil.SOLTEIRO);
        profissional.setStatusCadastro(status);
        profissional.setEmail(cpf + "@email.com");
        profissional.setCargo(cargo);
        if (profissional instanceof Especialista especialista) {
            especialista.setRegistroConselho("CRP-" + cpf);
            especialista.setEspecialidades(new ArrayList<>());
            especialista.setJornadaTrabalho(new ArrayList<>());
        }
        if (profissional instanceof EspecialistaPJ pj) {
            pj.setCnpj("000" + cpf);
            pj.setRazaoSocial("Clínica " + nome);
        }
        entityManager.persist(profissional);
    }
}
