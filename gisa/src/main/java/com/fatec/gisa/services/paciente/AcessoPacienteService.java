package com.fatec.gisa.services.paciente;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.repositories.PessoaRepository;
import com.fatec.gisa.repositories.assistentesocial.VinculoAssistenteSocialRepository;
import com.fatec.gisa.services.usuario.PermissaoUsuarioLogado;

import lombok.RequiredArgsConstructor;

// Escopo de acesso aos pacientes. VISUALIZAR_PACIENTES (ou ACESSO_TOTAL) vê todos; quem tem só
// VISUALIZAR_PACIENTES_VINCULADOS vê apenas os pacientes com vínculo ativo com ele (assistente social).
// O filtro é sempre feito aqui no back.
@Service
@RequiredArgsConstructor
public class AcessoPacienteService {

    public static final String VISUALIZAR_PACIENTES = "VISUALIZAR_PACIENTES";
    public static final String VISUALIZAR_PACIENTES_VINCULADOS = "VISUALIZAR_PACIENTES_VINCULADOS";

    // Sem restrição: o usuário vê todos os pacientes
    public static final long SEM_RESTRICAO = 0L;
    // Usuário restrito que não é uma pessoa cadastrada: não enxerga nenhum paciente
    private static final long NENHUM_PACIENTE = -1L;

    private final PessoaRepository pessoaRepository;
    private final VinculoAssistenteSocialRepository vinculoAssistenteSocialRepository;

    // SEM_RESTRICAO, ou o idCadastro do usuário logado quando ele só vê os pacientes vinculados a ele
    @Transactional(readOnly = true)
    public long restricaoDoUsuarioLogado() {
        if (PermissaoUsuarioLogado.possui(VISUALIZAR_PACIENTES)
                || !PermissaoUsuarioLogado.possui(VISUALIZAR_PACIENTES_VINCULADOS)) {
            return SEM_RESTRICAO;
        }
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return pessoaRepository.findByCpf(autenticacao.getName())
                .map(pessoa -> pessoa.getIdCadastro())
                .orElse(NENHUM_PACIENTE);
    }

    // Detalhes e histórico de um paciente: 403 se o usuário só vê os vinculados e não tem vínculo
    // ativo com este. Também vale para paciente inexistente, para não revelar quais IDs existem
    @Transactional(readOnly = true)
    public void exigirAcesso(Long idPaciente) {
        long restricao = restricaoDoUsuarioLogado();
        if (restricao != SEM_RESTRICAO && !vinculoAssistenteSocialRepository
                .existsByPaciente_IdCadastroAndAssistenteSocial_IdCadastroAndDataFimIsNull(idPaciente, restricao)) {
            throw new AccessDeniedException("Paciente não vinculado ao usuário logado.");
        }
    }
}
