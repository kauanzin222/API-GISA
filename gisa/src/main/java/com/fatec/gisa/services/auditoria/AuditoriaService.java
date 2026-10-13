package com.fatec.gisa.services.auditoria;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.auditoria.LogAuditoriaDTO;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.auditoria.LogAuditoria;
import com.fatec.gisa.enums.AcaoAuditoria;
import com.fatec.gisa.repositories.PessoaRepository;
import com.fatec.gisa.repositories.auditoria.LogAuditoriaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Instant SEM_INICIO = Instant.parse("2000-01-01T00:00:00Z");
    private static final Instant SEM_FIM = Instant.parse("9999-01-01T00:00:00Z");

    private final LogAuditoriaRepository logAuditoriaRepository;
    private final PessoaRepository pessoaRepository;

    // Transação própria: o registro não depende da transação da requisição auditada
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String cpf, AcaoAuditoria acao, String metodo, String caminho, int statusHttp, String ip) {
        logAuditoriaRepository.save(new LogAuditoria(
                null, Instant.now(), cpf, acao, metodo, limitar(caminho, 255), statusHttp, limitar(ip, 45)));
    }

    // Datas no fuso de Brasília; "fim" inclui o dia inteiro
    @Transactional(readOnly = true)
    public Page<LogAuditoriaDTO> listar(String cpf, List<AcaoAuditoria> acoes,
            LocalDate inicio, LocalDate fim, Pageable pageable) {
        Collection<AcaoAuditoria> acoesFiltradas = acoes == null || acoes.isEmpty()
                ? EnumSet.allOf(AcaoAuditoria.class)
                : acoes;
        Instant inicioInstante = inicio == null ? SEM_INICIO : inicio.atStartOfDay(FUSO).toInstant();
        Instant fimInstante = fim == null ? SEM_FIM : fim.plusDays(1).atStartOfDay(FUSO).toInstant();
        String cpfFiltro = cpf == null ? "" : cpf.replaceAll("\\D", "");

        Page<LogAuditoria> pagina = cpfFiltro.isEmpty()
                ? logAuditoriaRepository.buscarSemFiltroDeCpf(acoesFiltradas, inicioInstante, fimInstante, pageable)
                : logAuditoriaRepository.buscar(cpfFiltro, acoesFiltradas, inicioInstante, fimInstante, pageable);

        // Nomes buscados de uma vez para a página inteira
        Map<String, String> nomesPorCpf = pessoaRepository.findByCpfIn(pagina.getContent().stream()
                        .map(LogAuditoria::getCpfUsuario)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Pessoa::getCpf, Pessoa::getNome, (a, b) -> a));

        return pagina.map(log -> new LogAuditoriaDTO(
                log.getIdLog(),
                log.getDataHora(),
                log.getCpfUsuario(),
                log.getCpfUsuario() == null ? null : nomesPorCpf.get(log.getCpfUsuario()),
                log.getAcao(),
                log.getMetodo(),
                log.getCaminho(),
                log.getStatusHttp(),
                log.getStatusHttp() < 400,
                log.getIp()));
    }

    private String limitar(String valor, int tamanho) {
        return valor == null || valor.length() <= tamanho ? valor : valor.substring(0, tamanho);
    }
}
