package com.fatec.gisa.services.paciente;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.EnderecoDTO;
import com.fatec.gisa.dtos.FiltroPessoaDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelPatchRequestDTO;
import com.fatec.gisa.dtos.paciente.response.ResponsavelDetalheDTO;
import com.fatec.gisa.dtos.paciente.response.ResponsavelListaDTO;
import com.fatec.gisa.entities.Endereco;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.entities.paciente.Responsavel;
import com.fatec.gisa.entities.paciente.VinculoResponsavel;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.enums.TipoEventoPaciente;
import com.fatec.gisa.exceptions.CampoInvalidoException;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.repositories.PessoaRepository;
import com.fatec.gisa.repositories.paciente.ResponsavelRepository;
import com.fatec.gisa.repositories.paciente.VinculoResponsavelRepository;
import com.fatec.gisa.repositories.usuario.UsuarioRepository;
import com.fatec.gisa.services.PessoaMapper;
import com.fatec.gisa.services.PessoaService;
import com.fatec.gisa.services.endereco.EnderecoService;
import com.fatec.gisa.services.endereco.Moradia;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResponsavelService {

    private final ResponsavelRepository responsavelRepository;
    private final VinculoResponsavelRepository vinculoResponsavelRepository;
    private final UsuarioRepository usuarioRepository;
    private final PessoaRepository pessoaRepository;
    private final PessoaMapper pessoaMapper;
    private final EnderecoService enderecoService;
    private final PessoaService pessoaService;
    private final HistoricoPacienteService historicoPacienteService;

    // Responsáveis já cadastrados são reaproveitados sem alterar seus dados. Um responsável novo que não
    // reside com o paciente recebe o endereço próprio enviado para ele (no máximo 1, que não pode ser um
    // endereço do paciente); o que reside recebe os do paciente depois, em aplicarResidencia
    @Transactional
    public Responsavel obterOuCriar(ResponsavelCadastroRequestDTO dto, Paciente paciente, boolean reside) {
        if (!reside) {
            validarEnderecoProprio(dto, paciente);
        }
        if (dto.getIdResponsavel() != null) {
            Responsavel responsavel = responsavelRepository.findById(dto.getIdResponsavel())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Responsável não encontrado com ID: " + dto.getIdResponsavel()));
            validarCpf(dto, responsavel);
            return responsavel;
        }

        Responsavel responsavelExistente = responsavelRepository.findByCpf(dto.getCpf()).orElse(null);
        if (responsavelExistente != null) {
            return responsavelExistente;
        }

        if (pessoaRepository.existsByCpf(dto.getCpf())) {
            throw new IllegalArgumentException(
                    "O CPF informado pertence a uma pessoa que não está cadastrada como responsável");
        }

        Responsavel responsavel = new Responsavel();
        pessoaMapper.preencherPessoa(responsavel, dto);
        responsavel.setOcupacao(dto.getOcupacao());
        Responsavel responsavelSalvo = responsavelRepository.save(responsavel);
        if (!reside) {
            enderecoService.sincronizar(responsavelSalvo, dto.getEnderecos(), Moradia.sozinho());
        }
        return responsavelSalvo;
    }

    // RN06: quem não reside tem no máximo 1 endereço próprio, e ele não pode ser um endereço do paciente
    // (pela residência por endereço, dividir a linha com o paciente é residir)
    private void validarEnderecoProprio(ResponsavelCadastroRequestDTO dto, Paciente paciente) {
        List<EnderecoDTO> enderecos = dto.getEnderecos();
        if (enderecos == null || enderecos.isEmpty()) {
            return;
        }
        if (enderecos.size() > 1) {
            throw new CampoInvalidoException("enderecos",
                    "O responsável que não reside com o paciente pode ter no máximo 1 endereço.");
        }
        if (enderecosDe(paciente.getEnderecos()).stream()
                .anyMatch(linha -> enderecoService.mesmaChave(enderecos.get(0), linha))) {
            throw new CampoInvalidoException("enderecos",
                    "Este é um endereço do paciente. Para morar nele, marque que reside.");
        }
    }

    /**
     * Deixa o responsável ligado exatamente aos endereços do paciente em {@code alvo} (RN01) e ajusta o resto,
     * depois de o paciente ter sincronizado os endereços dele. Vazio = não reside.
     * <ul>
     * <li>sai das linhas do paciente fora do alvo e das que o paciente removeu (RN03, RN06);</li>
     * <li>entra nas do alvo, inclusive as que acabaram de ser criadas (RN04);</li>
     * <li>quem passa a residir agora perde os outros endereços, menos os que divide com outro paciente
     * dele (RN05).</li>
     * </ul>
     * A coluna {@code ResideComPaciente} acompanha o resultado (reside = divide ao menos uma linha).
     *
     * @param residiaAntes se ele já dividia alguma linha com o paciente antes do request
     * @param linhasRemovidas IDs das linhas que o paciente deixou de ter neste request, sem terem sido alteradas
     */
    @Transactional
    public void aplicarResidencia(Responsavel responsavel, VinculoResponsavel vinculo, Paciente paciente,
            List<Endereco> alvo, boolean residiaAntes, Set<Long> linhasRemovidas) {
        Set<Long> idsAlvo = idsDe(alvo);
        Set<Long> idsDoPaciente = idsDe(paciente.getEnderecos());

        for (Endereco linha : new ArrayList<>(enderecosDe(responsavel.getEnderecos()))) {
            Long id = linha.getIdEndereco();
            boolean saiDaLinha = (idsDoPaciente.contains(id) && !idsAlvo.contains(id))
                    || linhasRemovidas.contains(id);
            if (saiDaLinha) {
                enderecoService.desvincular(responsavel, linha);
            }
        }
        alvo.forEach(linha -> enderecoService.vincular(responsavel, linha));

        if (!alvo.isEmpty() && !residiaAntes) {
            removerEnderecosProprios(responsavel, paciente);
        }

        vinculo.setResideComPaciente(!alvo.isEmpty());
        revisarResidencias(responsavel, paciente.getIdCadastro());
    }

    // RN05: o endereço antigo é substituído, mas o responsável continua na casa de um irmão
    private void removerEnderecosProprios(Responsavel responsavel, Paciente paciente) {
        Set<Long> idsDoPaciente = idsDe(paciente.getEnderecos());
        for (Endereco linha : new ArrayList<>(enderecosDe(responsavel.getEnderecos()))) {
            if (!idsDoPaciente.contains(linha.getIdEndereco())
                    && !ehDeUmPacienteDele(responsavel, linha, paciente.getIdCadastro())) {
                enderecoService.desvincular(responsavel, linha);
            }
        }
    }

    // A linha também é de um paciente de quem o responsável cuida (menos o ignorado). Lê os moradores da linha e os
    // vínculos deles, que a sincronização mantém iguais em memória (a lista "dependentes" do responsável não)
    private static boolean ehDeUmPacienteDele(Responsavel responsavel, Endereco linha, Long idPacienteIgnorado) {
        if (linha.getMoradores() == null) {
            return false;
        }
        return linha.getMoradores().stream()
                .map(Hibernate::unproxy)
                .filter(Paciente.class::isInstance)
                .map(Paciente.class::cast)
                .filter(paciente -> !paciente.getIdCadastro().equals(idPacienteIgnorado))
                .anyMatch(paciente -> paciente.getResponsaveis() != null && paciente.getResponsaveis().stream()
                        .anyMatch(vinculo -> vinculo.getResponsavel().getIdCadastro()
                                .equals(responsavel.getIdCadastro())));
    }

    // A residência é derivada dos endereços: nos outros pacientes do responsável, a coluna passa a refletir
    // se ele ainda divide alguma linha com eles (RN07)
    private void revisarResidencias(Responsavel responsavel, Long idPacienteIgnorado) {
        if (responsavel.getDependentes() == null) {
            return;
        }
        for (VinculoResponsavel vinculo : responsavel.getDependentes()) {
            Paciente paciente = vinculo.getPaciente();
            if (paciente.getIdCadastro().equals(idPacienteIgnorado)) {
                continue;
            }
            boolean reside = !enderecoService.idsCompartilhados(responsavel, paciente).isEmpty();
            if (vinculo.isResideComPaciente() == reside) {
                continue;
            }
            vinculo.setResideComPaciente(reside);
            historicoPacienteService.registrar(paciente, TipoEventoPaciente.DADOS_ALTERADOS,
                    "Dados do cadastro alterados: residência de %s.".formatted(responsavel.getNome()));
        }
    }

    @Transactional(readOnly = true)
    public Page<ResponsavelListaDTO> listarPaginado(FiltroPessoaDTO filtro, Pageable pageable) {
        return responsavelRepository.buscarPorCpfENome(filtro.cpf(), filtro.nome(), pageable)
                .map(responsavel -> new ResponsavelListaDTO(
                        responsavel.getIdCadastro(),
                        responsavel.getNome(),
                        responsavel.getCpf(),
                        responsavel.getCelular(),
                        responsavel.getOcupacao(),
                        responsavel.getStatusCadastro()));
    }

    @Transactional(readOnly = true)
    public ResponsavelDetalheDTO buscarDetalhes(Long idResponsavel) {
        Responsavel responsavel = buscarResponsavel(idResponsavel);
        return new ResponsavelDetalheDTO(
                pessoaMapper.toPessoaResponseDTO(responsavel),
                responsavel.getOcupacao());
    }

    @Transactional
    public ResponsavelDetalheDTO atualizar(Long idResponsavel, ResponsavelPatchRequestDTO dto) {
        Responsavel responsavel = buscarResponsavel(idResponsavel);
        if (dto.hasField("enderecos")) {
            validarEnderecosDoPatch(responsavel, dto.getEnderecos());
        }
        pessoaService.aplicarAlteracoes(responsavel, dto);

        if (dto.hasField("ocupacao")) {
            responsavel.setOcupacao(dto.getOcupacao());
        }
        if (dto.hasField("enderecos")) {
            revisarResidencias(responsavel, null);
        }

        responsavelRepository.save(responsavel);
        return new ResponsavelDetalheDTO(
                pessoaMapper.toPessoaResponseDTO(responsavel),
                responsavel.getOcupacao());
    }

    // RN08: por aqui só se gerenciam os endereços que não são de nenhum paciente dele (no máximo 1). Um endereço
    // que também é de um paciente tem dono no paciente: pode vir igual ao atual, mas não alterado nem omitido
    private void validarEnderecosDoPatch(Responsavel responsavel, List<EnderecoDTO> lista) {
        List<EnderecoDTO> proprios = new ArrayList<>(lista == null ? List.of() : lista);

        for (Endereco linha : enderecosDe(responsavel.getEnderecos())) {
            if (!ehDeUmPacienteDele(responsavel, linha, null)) {
                continue;
            }
            EnderecoDTO item = proprios.stream()
                    .filter(candidato -> Objects.equals(candidato.getIdEndereco(), linha.getIdEndereco())
                            || (candidato.getIdEndereco() == null && enderecoService.mesmaChave(candidato, linha)))
                    .findFirst()
                    .orElseThrow(ResponsavelService::enderecoDePaciente);
            if (!enderecoService.iguais(item, linha)) {
                throw enderecoDePaciente();
            }
            // Comparado por identidade: o equals do DTO é por valor
            proprios.removeIf(candidato -> candidato == item);
        }
        if (proprios.size() > 1) {
            throw new CampoInvalidoException("enderecos", "O responsável pode ter no máximo 1 endereço próprio.");
        }
    }

    private static CampoInvalidoException enderecoDePaciente() {
        return new CampoInvalidoException("enderecos",
                "Este endereço também é de um paciente. Altere pela edição do paciente.");
    }

    @Transactional
    public void inativar(Long idResponsavel) {
        Responsavel responsavel = buscarResponsavel(idResponsavel);
        responsavel.setStatusCadastro(StatusCadastro.INATIVO);
    }

    @Transactional
    public void removerOrfaosSemVinculos(Iterable<Responsavel> responsaveis) {
        for (Responsavel responsavel : responsaveis) {
            Long idResponsavel = responsavel.getIdCadastro();
            if (vinculoResponsavelRepository.existsByResponsavel_IdCadastro(idResponsavel)
                    || usuarioRepository.existsById(idResponsavel)) {
                continue;
            }

            enderecoService.desvincularTodos(responsavel);
            responsavelRepository.delete(responsavel);
        }
    }

    private Responsavel buscarResponsavel(Long idResponsavel) {
        return responsavelRepository.findById(idResponsavel)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Responsável não encontrado com ID: " + idResponsavel));
    }

    private void validarCpf(ResponsavelCadastroRequestDTO dto, Responsavel responsavel) {
        if (!responsavel.getCpf().equals(dto.getCpf())) {
            throw new IllegalArgumentException("O CPF informado não corresponde ao responsável selecionado");
        }
    }

    private static List<Endereco> enderecosDe(List<Endereco> enderecos) {
        return enderecos == null ? List.of() : enderecos;
    }

    private static Set<Long> idsDe(List<Endereco> enderecos) {
        return enderecosDe(enderecos).stream().map(Endereco::getIdEndereco).collect(Collectors.toSet());
    }
}
