package com.fatec.gisa.services.paciente;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.paciente.request.EscolaCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.EscolaPatchRequestDTO;
import com.fatec.gisa.dtos.paciente.response.EscolaDetalheDTO;
import com.fatec.gisa.dtos.paciente.response.EscolaResponseDTO;
import com.fatec.gisa.entities.paciente.Escola;
import com.fatec.gisa.enums.TipoEscola;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.repositories.paciente.EscolaRepository;
import com.fatec.gisa.repositories.paciente.PacienteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EscolaService {

    private final EscolaRepository escolaRepository;
    private final PacienteRepository pacienteRepository;

    @Transactional(readOnly = true)
    public Escola buscarOpcional(Long idEscola) {
        if (idEscola == null) {
            return null;
        }

        return escolaRepository.findById(idEscola)
                .orElseThrow(() -> new IllegalArgumentException("Escola não encontrada com ID: " + idEscola));
    }

    @Transactional(readOnly = true)
    // Sem nome (ou em branco), devolve as 10 primeiras em ordem alfabética
    public List<EscolaResponseDTO> buscarSugestoes(String nome) {
        String trecho = nome == null ? "" : nome.trim();

        return escolaRepository.findTop10ByNomeContainingIgnoreCaseOrderByNomeAsc(trecho)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // nome e tipoEscola nulos não restringem a busca
    @Transactional(readOnly = true)
    public Page<EscolaDetalheDTO> listarPaginado(String nome, TipoEscola tipoEscola, Pageable pageable) {
        Page<Escola> pagina = escolaRepository.buscar(nome == null ? "" : nome.trim(),
                tipoEscola == null ? EnumSet.allOf(TipoEscola.class) : EnumSet.of(tipoEscola), pageable);

        List<Long> ids = pagina.getContent().stream().map(Escola::getIdEscola).toList();
        Map<Long, Long> pacientesPorEscola = ids.isEmpty()
                ? Map.of()
                : pacienteRepository.contarPorEscola(ids).stream()
                        .collect(Collectors.toMap(linha -> (Long) linha[0], linha -> (Long) linha[1]));

        return pagina.map(escola -> toDetalheDTO(escola, pacientesPorEscola.getOrDefault(escola.getIdEscola(), 0L)));
    }

    @Transactional(readOnly = true)
    public EscolaDetalheDTO buscarDetalhes(Long idEscola) {
        Escola escola = buscarEscola(idEscola);
        long quantidade = pacienteRepository.contarPorEscola(List.of(idEscola)).stream()
                .findFirst()
                .map(linha -> (Long) linha[1])
                .orElse(0L);
        return toDetalheDTO(escola, quantidade);
    }

    @Transactional
    public EscolaResponseDTO cadastrar(EscolaCadastroRequestDTO dto) {
        Escola escola = new Escola();
        escola.setNome(dto.getNome().trim());
        escola.setTipoEscola(dto.getTipoEscola());
        escola.setTelefone(dto.getTelefone());

        return toResponseDTO(escolaRepository.save(escola));
    }

    @Transactional
    public Escola atualizar(Long idEscola, EscolaPatchRequestDTO dto) {
        Escola escola = buscarEscola(idEscola);

        if (dto.hasField("nome")) {
            if (dto.getNome() == null || dto.getNome().isBlank()) {
                throw new IllegalArgumentException("O nome da escola não pode ser vazio.");
            }
            escola.setNome(dto.getNome().trim());
        }
        if (dto.hasField("tipoEscola")) {
            if (dto.getTipoEscola() == null) {
                throw new IllegalArgumentException("O tipo da escola não pode ser nulo.");
            }
            escola.setTipoEscola(dto.getTipoEscola());
        }
        if (dto.hasField("telefone")) {
            escola.setTelefone(dto.getTelefone());
        }

        return escolaRepository.save(escola);
    }

    // Escolas não têm status: o DELETE exclui, e só sem pacientes vinculados (nem inativos)
    @Transactional
    public void excluir(Long idEscola) {
        Escola escola = buscarEscola(idEscola);
        if (pacienteRepository.existsByEscola_IdEscola(idEscola)) {
            throw new IllegalArgumentException("Não é possível excluir a escola: há pacientes vinculados a ela.");
        }
        escolaRepository.delete(escola);
    }

    private Escola buscarEscola(Long idEscola) {
        return escolaRepository.findById(idEscola)
                .orElseThrow(() -> new ResourceNotFoundException("Escola não encontrada com ID: " + idEscola));
    }

    private EscolaResponseDTO toResponseDTO(Escola escola) {
        return new EscolaResponseDTO(
                escola.getIdEscola(),
                escola.getNome(),
                escola.getTipoEscola(),
                escola.getTelefone());
    }

    private EscolaDetalheDTO toDetalheDTO(Escola escola, long quantidadePacientes) {
        return new EscolaDetalheDTO(
                escola.getIdEscola(),
                escola.getNome(),
                escola.getTipoEscola(),
                escola.getTelefone(),
                quantidadePacientes);
    }
}
