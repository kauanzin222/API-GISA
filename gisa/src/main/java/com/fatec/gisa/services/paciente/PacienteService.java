package com.fatec.gisa.services.paciente;

import java.time.LocalDate;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.paciente.request.PacientePatchRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ProntuarioPatchRequestDTO;
import com.fatec.gisa.dtos.paciente.request.PacienteCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelVinculoPatchDTO;
import com.fatec.gisa.dtos.paciente.response.CidResponseDTO;
import com.fatec.gisa.dtos.assistentesocial.response.AssistenteSocialResumoDTO;
import com.fatec.gisa.dtos.paciente.response.EscolaResponseDTO;
import com.fatec.gisa.dtos.paciente.response.PacienteDetalheDTO;
import com.fatec.gisa.dtos.paciente.response.InativacaoPacienteDTO;
import com.fatec.gisa.dtos.paciente.response.PacienteBuscaDTO;
import com.fatec.gisa.dtos.paciente.response.ProntuarioResponseDTO;
import com.fatec.gisa.dtos.paciente.response.ResponsavelVinculoResponseDTO;
import com.fatec.gisa.dtos.EnderecoDTO;
import com.fatec.gisa.entities.Endereco;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.entities.paciente.Cid;
import com.fatec.gisa.entities.paciente.Escola;
import com.fatec.gisa.entities.paciente.Prontuario;
import com.fatec.gisa.entities.paciente.Responsavel;
import com.fatec.gisa.entities.paciente.VinculoResponsavel;
import com.fatec.gisa.enums.GrauParentesco;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.enums.StatusPaciente;
import com.fatec.gisa.enums.TipoEventoPaciente;
import com.fatec.gisa.repositories.PessoaRepository;
import com.fatec.gisa.repositories.assistentesocial.VinculoAssistenteSocialRepository;
import com.fatec.gisa.repositories.paciente.PacienteRepository;
import com.fatec.gisa.repositories.paciente.ResponsavelRepository;
import com.fatec.gisa.repositories.paciente.VinculoResponsavelRepository;
import com.fatec.gisa.exceptions.CampoInvalidoException;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.services.PessoaMapper;
import com.fatec.gisa.services.PessoaService;
import com.fatec.gisa.services.endereco.EnderecoService;
import com.fatec.gisa.services.endereco.Moradia;
import com.fatec.gisa.services.terapia.TerapiaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final VinculoResponsavelRepository vinculoResponsavelRepository;
    private final PessoaRepository pessoaRepository;
    private final ResponsavelRepository responsavelRepository;
    private final PessoaMapper pessoaMapper;
    private final EnderecoService enderecoService;
    private final PessoaService pessoaService;
    private final EscolaService escolaService;
    private final CidService cidService;
    private final ProntuarioService prontuarioService;
    private final ResponsavelService responsavelService;
    private final HistoricoPacienteService historicoPacienteService;
    private final AcessoPacienteService acessoPacienteService;
    private final VinculoAssistenteSocialRepository vinculoAssistenteSocialRepository;
    private final TerapiaService terapiaService;

    private static final int LIMITE_BUSCA = 20;
    private static final int TAMANHO_MINIMO_BUSCA_NOME = 3;

    // Busca de pacientes (LGPD): não há listagem completa, só se encontra um paciente buscando por ele.
    // "termo" só com dígitos (ignorando "." e "-") é CPF exato com 11 dígitos; senão, trecho do nome
    // com ao menos 3 caracteres, sem diferenciar acento nem maiúsculas. Com idEscola (tela da escola),
    // o termo é opcional. Sem paginação: no máximo 20 resultados, ordenados por nome. Quem só vê os
    // pacientes vinculados recebe só esses
    @Transactional(readOnly = true)
    public List<PacienteBuscaDTO> buscar(String termo, Long idEscola) {
        if (idEscola != null && idEscola <= 0) {
            throw new IllegalArgumentException("Valor inválido para 'idEscola': " + idEscola);
        }
        String texto = termo == null ? "" : termo.trim();
        if (texto.isEmpty() && idEscola == null) {
            throw new IllegalArgumentException("O parâmetro 'termo' é obrigatório.");
        }

        String cpf = "";
        String nome = "";
        if (!texto.isEmpty()) {
            String semMascara = texto.replaceAll("[.\\-]", "");
            if (semMascara.matches("\\d+")) {
                if (semMascara.length() != 11) {
                    throw new IllegalArgumentException("A busca por CPF exige os 11 dígitos.");
                }
                cpf = semMascara;
            } else {
                if (texto.length() < TAMANHO_MINIMO_BUSCA_NOME) {
                    throw new IllegalArgumentException("A busca por nome exige ao menos 3 caracteres.");
                }
                nome = semAcento(texto).toLowerCase(Locale.ROOT);
            }
        }

        List<Long> ids = pacienteRepository.buscarIds(cpf, nome,
                PacienteRepository.LETRAS_ACENTUADAS, PacienteRepository.LETRAS_SEM_ACENTO,
                idEscola == null ? 0L : idEscola, acessoPacienteService.restricaoDoUsuarioLogado(), LIMITE_BUSCA);
        // findAllById não garante a ordem: volta para a ordem da busca (por nome)
        Map<Long, Paciente> pacientesPorId = pacienteRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Paciente::getIdCadastro, paciente -> paciente));
        return ids.stream()
                .map(pacientesPorId::get)
                .filter(Objects::nonNull)
                .map(PacienteService::toBuscaDTO)
                .toList();
    }

    public static PacienteBuscaDTO toBuscaDTO(Paciente paciente) {
        return new PacienteBuscaDTO(
                paciente.getIdCadastro(),
                paciente.getNome(),
                mascararCpf(paciente.getCpf()),
                paciente.getStatusCadastro());
    }

    // "45678901234" -> "***.789.012-**"; fora do formato de 11 dígitos, nada do CPF aparece
    static String mascararCpf(String cpf) {
        String digitos = cpf == null ? "" : cpf.replaceAll("\\D", "");
        if (digitos.length() != 11) {
            return "***.***.***-**";
        }
        return "***." + digitos.substring(3, 6) + "." + digitos.substring(6, 9) + "-**";
    }

    private static String semAcento(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    @Transactional(readOnly = true)
    public PacienteDetalheDTO buscarDetalhes(Long idCadastro) {
        // Quem só vê os pacientes vinculados recebe 403 para os demais
        acessoPacienteService.exigirAcesso(idCadastro);
        Paciente paciente = pacienteRepository.findById(idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Paciente não encontrado com ID: " + idCadastro));

        List<CidResponseDTO> cids = paciente.getCids() == null
                ? List.of()
                : paciente.getCids().stream()
                        .sorted(Comparator.comparing(Cid::getCodigoCID))
                        .map(cid -> new CidResponseDTO(cid.getCodigoCID(), cid.getDescricao()))
                        .toList();

        List<ResponsavelVinculoResponseDTO> responsaveis = paciente.getResponsaveis() == null
                ? List.of()
                : paciente.getResponsaveis().stream()
                        .map(vinculo -> {
                            // A residência é derivada: o responsável reside nos endereços que divide com o paciente
                            List<Long> idsResidencia = enderecoService.idsCompartilhados(
                                    paciente, vinculo.getResponsavel());
                            return new ResponsavelVinculoResponseDTO(
                                    pessoaMapper.toPessoaResponseDTO(vinculo.getResponsavel()),
                                    vinculo.getResponsavel().getOcupacao(),
                                    vinculo.getGrauParentesco(),
                                    !idsResidencia.isEmpty(),
                                    idsResidencia);
                        })
                        .toList();

        Escola escola = paciente.getEscola();
        EscolaResponseDTO escolaResponse = escola == null
                ? null
                : new EscolaResponseDTO(
                        escola.getIdEscola(),
                        escola.getNome(),
                        escola.getTipoEscola(),
                        escola.getTelefone());

        Prontuario prontuario = paciente.getProntuario();
        ProntuarioResponseDTO prontuarioResponse = prontuario == null
                ? null
                : new ProntuarioResponseDTO(
                        prontuario.getAlergias(),
                        prontuario.getComorbidade(),
                        prontuario.getMobilidade());

        return new PacienteDetalheDTO(
                pessoaMapper.toPessoaResponseDTO(paciente),
                paciente.getStatusPaciente(),
                paciente.getDataCadastro(),
                paciente.getConvenio(),
                escolaResponse,
                cids,
                prontuarioResponse,
                responsaveis,
                vinculoAssistenteSocialRepository.buscarAtivosDoPaciente(idCadastro).stream()
                        .map(vinculo -> new AssistenteSocialResumoDTO(
                                vinculo.getAssistenteSocial().getIdCadastro(),
                                vinculo.getAssistenteSocial().getNome()))
                        .toList());
    }

    @Transactional
    public Paciente atualizar(Long idCadastro, PacientePatchRequestDTO dto) {
        Paciente paciente = pacienteRepository.findById(idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Paciente não encontrado com ID: " + idCadastro));
        // Para o histórico: só o que realmente mudou vira evento (o front costuma reenviar o formulário inteiro)
        DadosParaHistorico antes = DadosParaHistorico.de(paciente, pessoaMapper);
        Set<Long> enderecosAntes = idsDosEnderecos(paciente);
        Map<Long, Set<Long>> compartilhadasAntes = linhasCompartilhadas(paciente);
        List<EnderecoDTO> itensEnderecos = dto.hasField("enderecos") ? dto.getEnderecos() : null;
        Map<Long, Pedido> pedidos = pedidosDoPatch(paciente, dto, itensEnderecos);

        pessoaService.aplicarAlteracoes(paciente, dto, moradiaDosResponsaveis(pedidos, itensEnderecos));

        if (dto.hasField("statusPaciente")) {
            if (dto.getStatusPaciente() == null) {
                throw new IllegalArgumentException("O status do paciente não pode ser nulo.");
            }
            paciente.setStatusPaciente(dto.getStatusPaciente());
        }
        if (dto.hasField("convenio")) {
            paciente.setConvenio(dto.getConvenio());
        }
        if (dto.hasField("idEscola")) {
            paciente.setEscola(escolaService.buscarOpcional(dto.getIdEscola()));
        }
        if (dto.hasField("codigosCid") || dto.hasField("novosCids")) {
            // Sem codigosCid, os CIDs atuais são mantidos e os novos são somados a eles
            List<String> codigos = dto.hasField("codigosCid")
                    ? (dto.getCodigosCid() == null ? List.of() : dto.getCodigosCid())
                    : paciente.getCids() == null ? List.of()
                            : paciente.getCids().stream().map(Cid::getCodigoCID).toList();
            paciente.setCids(cidService.obterOuCriar(codigos,
                    dto.getNovosCids() == null ? List.of() : dto.getNovosCids()));
        }
        if (dto.hasField("prontuario")) {
            atualizarProntuario(paciente, dto.getProntuario());
        }
        List<Responsavel> responsaveisDesvinculados = List.of();
        if (dto.hasField("responsaveis")) {
            responsaveisDesvinculados = substituirResponsaveis(paciente, dto.getResponsaveis());
        }
        aplicarResidencias(paciente, pedidos, itensEnderecos, enderecosAntes, compartilhadasAntes);

        Paciente pacienteAtualizado = pacienteRepository.save(paciente);
        registrarAlteracoes(pacienteAtualizado, antes, DadosParaHistorico.de(pacienteAtualizado, pessoaMapper));
        if (!responsaveisDesvinculados.isEmpty()) {
            vinculoResponsavelRepository.flush();
            responsavelService.removerOrfaosSemVinculos(responsaveisDesvinculados);
        }
        return pacienteAtualizado;
    }

    // ---------- Residência dos responsáveis por endereço do paciente ----------

    // Como o request diz onde o responsável mora: INDICES (posições na lista "enderecos" do request), IDS (IDs de
    // endereços atuais do paciente), TODOS e NENHUM (formato antigo: reside / não reside) ou MANTER (sem informação:
    // continua onde está)
    private enum Modo { INDICES, IDS, TODOS, NENHUM, MANTER }

    private record Pedido(Modo modo, Set<Integer> indices, Set<Long> ids) {
        static final Pedido TODOS = new Pedido(Modo.TODOS, Set.of(), Set.of());
        static final Pedido NENHUM = new Pedido(Modo.NENHUM, Set.of(), Set.of());
        static final Pedido MANTER = new Pedido(Modo.MANTER, Set.of(), Set.of());
    }

    private static Pedido pedidoDeIndices(List<Integer> indices, int quantidade, String campo) {
        for (Integer indice : indices) {
            if (indice == null || indice < 0 || indice >= quantidade) {
                throw new CampoInvalidoException(campo, "Índice de endereço fora da lista de endereços.");
            }
        }
        return new Pedido(Modo.INDICES, new HashSet<>(indices), Set.of());
    }

    // POST /pacientes: os índices apontam para a lista "enderecos" do próprio request. Sem eles, vale o formato antigo
    // (RN09). Os endereços do paciente já foram sincronizados
    private List<Endereco> residenciaDoCadastro(Paciente paciente, List<EnderecoDTO> itens,
            ResponsavelCadastroRequestDTO dto, int posicao) {
        if (dto.getIdsEnderecoResidencia() != null) {
            throw new CampoInvalidoException("responsaveis[%d].idsEnderecoResidencia".formatted(posicao),
                    "No cadastro do paciente, informe indicesEnderecoResidencia.");
        }
        Pedido pedido = dto.getIndicesEnderecoResidencia() != null
                ? pedidoDeIndices(dto.getIndicesEnderecoResidencia(), itens == null ? 0 : itens.size(),
                        "responsaveis[%d].indicesEnderecoResidencia".formatted(posicao))
                : formatoAntigo(dto.getNaoResideComPaciente());
        return alvo(pedido, paciente, itens, null, Set.of(), Set.of());
    }

    // POST /pacientes/{id}/responsaveis: os IDs são de endereços atuais do paciente. Sem eles, vale o formato antigo
    private List<Endereco> residenciaDoVinculoNovo(Paciente paciente, ResponsavelCadastroRequestDTO dto) {
        if (dto.getIndicesEnderecoResidencia() != null) {
            throw new CampoInvalidoException("indicesEnderecoResidencia",
                    "Neste endpoint, informe idsEnderecoResidencia.");
        }
        Pedido pedido;
        if (dto.getIdsEnderecoResidencia() != null) {
            Set<Long> ids = new HashSet<>(dto.getIdsEnderecoResidencia());
            if (ids.contains(null) || !idsDosEnderecos(paciente).containsAll(ids)) {
                throw new CampoInvalidoException("idsEnderecoResidencia", "O endereço informado não é do paciente.");
            }
            pedido = new Pedido(Modo.IDS, Set.of(), ids);
        } else {
            pedido = formatoAntigo(dto.getNaoResideComPaciente());
        }
        return alvo(pedido, paciente, null, null, Set.of(), Set.of());
    }

    private static Pedido formatoAntigo(Boolean naoResideComPaciente) {
        return Boolean.TRUE.equals(naoResideComPaciente) ? Pedido.NENHUM : Pedido.TODOS;
    }

    // PATCH: um pedido por responsável. Quem não é citado (ou não traz nada sobre residência) continua onde está
    private Map<Long, Pedido> pedidosDoPatch(Paciente paciente, PacientePatchRequestDTO dto,
            List<EnderecoDTO> itensEnderecos) {
        Map<Long, Pedido> pedidos = new HashMap<>();
        if (paciente.getResponsaveis() != null) {
            paciente.getResponsaveis()
                    .forEach(vinculo -> pedidos.put(vinculo.getResponsavel().getIdCadastro(), Pedido.MANTER));
        }
        if (!dto.hasField("responsaveis") || dto.getResponsaveis() == null) {
            return pedidos;
        }

        for (int posicao = 0; posicao < dto.getResponsaveis().size(); posicao++) {
            ResponsavelVinculoPatchDTO item = dto.getResponsaveis().get(posicao);
            if (item.indicesEnderecoResidencia() != null) {
                String campo = "responsaveis[%d].indicesEnderecoResidencia".formatted(posicao);
                if (itensEnderecos == null) {
                    throw new CampoInvalidoException(campo,
                            "Para informar a residência por endereço, envie também os endereços do paciente.");
                }
                pedidos.put(item.idResponsavel(),
                        pedidoDeIndices(item.indicesEnderecoResidencia(), itensEnderecos.size(), campo));
            } else if (item.resideComPaciente() != null) {
                pedidos.put(item.idResponsavel(), formatoAntigo(!item.resideComPaciente()));
            } else {
                // Vínculo novo sem informação de residência: não reside
                pedidos.putIfAbsent(item.idResponsavel(), Pedido.NENHUM);
            }
        }
        return pedidos;
    }

    // Os endereços do paciente em que cada responsável já mora (IDs das linhas que divide com ele)
    private Map<Long, Set<Long>> linhasCompartilhadas(Paciente paciente) {
        Map<Long, Set<Long>> compartilhadas = new HashMap<>();
        if (paciente.getResponsaveis() != null) {
            for (VinculoResponsavel vinculo : paciente.getResponsaveis()) {
                compartilhadas.put(vinculo.getResponsavel().getIdCadastro(), new HashSet<>(
                        enderecoService.idsCompartilhados(paciente, vinculo.getResponsavel())));
            }
        }
        return compartilhadas;
    }

    // Os endereços do paciente em que o responsável mora, segundo o pedido (depois de o paciente sincronizar os seus)
    private List<Endereco> alvo(Pedido pedido, Paciente paciente, List<EnderecoDTO> itens, Responsavel responsavel,
            Set<Long> compartilhadasAntes, Set<Long> idsLinhasAntes) {
        List<Endereco> linhas = paciente.getEnderecos() == null ? List.of() : paciente.getEnderecos();
        return switch (pedido.modo()) {
            case NENHUM -> List.of();
            case TODOS -> List.copyOf(linhas);
            case IDS -> linhas.stream().filter(linha -> pedido.ids().contains(linha.getIdEndereco())).toList();
            case INDICES -> {
                Set<Long> ids = pedido.indices().stream()
                        .map(indice -> enderecoService.linhaDe(paciente, itens.get(indice))
                                .orElseThrow(() -> new IllegalStateException(
                                        "Endereço do paciente não encontrado após a sincronização.")))
                        .map(Endereco::getIdEndereco)
                        .collect(Collectors.toSet());
                yield linhas.stream().filter(linha -> ids.contains(linha.getIdEndereco())).toList();
            }
            case MANTER -> {
                if (compartilhadasAntes.isEmpty()) {
                    yield List.of();
                }
                // Quem morava em todos os endereços (a regra antiga) continua morando em todos, inclusive nos novos
                if (compartilhadasAntes.containsAll(idsLinhasAntes)) {
                    yield List.copyOf(linhas);
                }
                yield linhas.stream()
                        .filter(linha -> linha.getMoradores() != null && linha.getMoradores().stream()
                                .anyMatch(morador -> Objects.equals(morador.getIdCadastro(),
                                        responsavel.getIdCadastro())))
                        .toList();
            }
        };
    }

    // Ao alterar um endereço, quem mora nele segundo o request vai junto (acompanhantes) e quem o request marca como
    // não morando nele fica no endereço antigo (ficam). Os dois casos não contam como "outros cadastros" no 409
    private Moradia moradiaDosResponsaveis(Map<Long, Pedido> pedidos, List<EnderecoDTO> itens) {
        return new Moradia() {
            @Override
            public Set<Pessoa> acompanhantes(Endereco linha) {
                return moradores(linha, true);
            }

            @Override
            public Set<Pessoa> ficam(Endereco linha) {
                return moradores(linha, false);
            }

            private Set<Pessoa> moradores(Endereco linha, boolean acompanham) {
                if (linha.getMoradores() == null) {
                    return Set.of();
                }
                return linha.getMoradores().stream()
                        .filter(morador -> {
                            Pedido pedido = pedidos.get(morador.getIdCadastro());
                            return pedido != null && segue(pedido, linha, itens) == acompanham;
                        })
                        .collect(Collectors.toSet());
            }
        };
    }

    private boolean segue(Pedido pedido, Endereco linha, List<EnderecoDTO> itens) {
        return switch (pedido.modo()) {
            case TODOS, MANTER -> true;
            case NENHUM -> false;
            case IDS -> pedido.ids().contains(linha.getIdEndereco());
            case INDICES -> pedido.indices().stream().map(itens::get)
                    .anyMatch(item -> Objects.equals(item.getIdEndereco(), linha.getIdEndereco())
                            || (item.getIdEndereco() == null && enderecoService.mesmaChave(item, linha)));
        };
    }

    // Depois de o paciente sincronizar os endereços e de os vínculos serem atualizados: cada responsável fica
    // ligado exatamente aos endereços pedidos (RN01), saindo também das linhas que o paciente removeu (RN03)
    private void aplicarResidencias(Paciente paciente, Map<Long, Pedido> pedidos, List<EnderecoDTO> itens,
            Set<Long> idsLinhasAntes, Map<Long, Set<Long>> compartilhadasAntes) {
        if (paciente.getResponsaveis() == null) {
            return;
        }
        Set<Long> removidas = new HashSet<>(idsLinhasAntes);
        removidas.removeAll(idsDosEnderecos(paciente));
        if (itens != null) {
            // Uma linha citada pelo ID foi alterada (quem não a acompanhou fica nela), não removida
            itens.stream().map(EnderecoDTO::getIdEndereco).filter(Objects::nonNull).forEach(removidas::remove);
        }

        for (VinculoResponsavel vinculo : new ArrayList<>(paciente.getResponsaveis())) {
            Responsavel responsavel = vinculo.getResponsavel();
            Long id = responsavel.getIdCadastro();
            Set<Long> antes = compartilhadasAntes.getOrDefault(id, Set.of());
            List<Endereco> alvo = alvo(pedidos.getOrDefault(id, Pedido.MANTER), paciente, itens, responsavel, antes,
                    idsLinhasAntes);
            responsavelService.aplicarResidencia(responsavel, vinculo, paciente, alvo, !antes.isEmpty(), removidas);
        }
    }

    private static Set<Long> idsDosEnderecos(Paciente paciente) {
        return paciente.getEnderecos() == null
                ? Set.of()
                : paciente.getEnderecos().stream().map(Endereco::getIdEndereco).collect(Collectors.toSet());
    }

    private void registrarAlteracoes(Paciente paciente, DadosParaHistorico antes, DadosParaHistorico depois) {
        if (antes.statusCadastro() != depois.statusCadastro()) {
            if (depois.statusCadastro() == StatusCadastro.INATIVO) {
                historicoPacienteService.registrar(paciente, TipoEventoPaciente.INATIVADO, "Cadastro inativado.");
            } else {
                historicoPacienteService.registrar(paciente, TipoEventoPaciente.REATIVADO, "Cadastro reativado.");
            }
        }
        registrarMudancaDeStatus(paciente, antes.statusPaciente(), depois.statusPaciente());

        List<String> alterados = antes.camposAlterados(depois);
        if (!alterados.isEmpty()) {
            historicoPacienteService.registrar(paciente, TipoEventoPaciente.DADOS_ALTERADOS,
                    "Dados do cadastro alterados: " + TextoHistorico.lista(alterados) + ".");
        }
        // O endereço em si não vai para o histórico
        if (!antes.enderecos().equals(depois.enderecos())) {
            historicoPacienteService.registrar(paciente, TipoEventoPaciente.ENDERECO_ALTERADO, "Endereço alterado.");
        }

        depois.responsaveis().forEach((idResponsavel, vinculo) -> {
            if (!antes.responsaveis().containsKey(idResponsavel)) {
                registrarVinculo(paciente, vinculo.nome(), vinculo.grau());
            }
        });
        antes.responsaveis().forEach((idResponsavel, vinculo) -> {
            if (!depois.responsaveis().containsKey(idResponsavel)) {
                historicoPacienteService.registrar(paciente, TipoEventoPaciente.RESPONSAVEL_DESVINCULADO,
                        "Responsável %s (%s) desvinculado.".formatted(
                                vinculo.nome(), TextoHistorico.parentesco(vinculo.grau())));
            }
        });
    }

    private void registrarMudancaDeStatus(Paciente paciente, StatusPaciente antes, StatusPaciente depois) {
        if (antes != depois) {
            historicoPacienteService.registrar(paciente, TipoEventoPaciente.STATUS_PACIENTE_ALTERADO,
                    "Status do paciente alterado de %s para %s.".formatted(
                            TextoHistorico.statusPaciente(antes), TextoHistorico.statusPaciente(depois)));
        }
    }

    private void registrarVinculo(Paciente paciente, String nome, GrauParentesco grau) {
        historicoPacienteService.registrar(paciente, TipoEventoPaciente.RESPONSAVEL_VINCULADO,
                "Responsável %s (%s) vinculado.".formatted(nome, TextoHistorico.parentesco(grau)));
    }

    @Transactional
    public Paciente adicionarResponsavel(Long idCadastro, ResponsavelCadastroRequestDTO dto) {
        Paciente paciente = pacienteRepository.findById(idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Paciente não encontrado com ID: " + idCadastro));

        List<Endereco> alvo = residenciaDoVinculoNovo(paciente, dto);
        Responsavel responsavel = responsavelService.obterOuCriar(dto, paciente, !alvo.isEmpty());

        if (paciente.getResponsaveis() == null) {
            paciente.setResponsaveis(new ArrayList<>());
        }

        boolean jaVinculado = paciente.getResponsaveis().stream()
                .anyMatch(vinculo -> vinculo.getResponsavel().getIdCadastro()
                        .equals(responsavel.getIdCadastro()));
        if (jaVinculado) {
            throw new IllegalArgumentException("Este responsável já está vinculado ao paciente.");
        }

        VinculoResponsavel vinculo = new VinculoResponsavel();
        vinculo.setPaciente(paciente);
        vinculo.setResponsavel(responsavel);
        vinculo.setGrauParentesco(dto.getGrauParentesco());
        paciente.getResponsaveis().add(vinculo);
        responsavelService.aplicarResidencia(responsavel, vinculo, paciente, alvo, false, Set.of());

        Paciente pacienteSalvo = pacienteRepository.save(paciente);
        registrarVinculo(pacienteSalvo, responsavel.getNome(), dto.getGrauParentesco());
        return pacienteSalvo;
    }

    // Na mesma transação, cuida das terapias agendadas futuras do paciente (ver TerapiaService).
    // Os vínculos com assistentes sociais não mudam
    @Transactional
    public InativacaoPacienteDTO inativar(Long idCadastro) {
        Paciente paciente = pacienteRepository.findById(idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Paciente não encontrado com ID: " + idCadastro));
        StatusCadastro statusCadastroAnterior = paciente.getStatusCadastro();
        StatusPaciente statusPacienteAnterior = paciente.getStatusPaciente();

        paciente.setStatusCadastro(StatusCadastro.INATIVO);
        if (paciente.getStatusPaciente() != StatusPaciente.ALTA) {
            paciente.setStatusPaciente(StatusPaciente.DESLIGADO);
        }

        if (statusCadastroAnterior != StatusCadastro.INATIVO) {
            historicoPacienteService.registrar(paciente, TipoEventoPaciente.INATIVADO, "Cadastro inativado.");
        }
        registrarMudancaDeStatus(paciente, statusPacienteAnterior, paciente.getStatusPaciente());

        TerapiaService.RetiradaDeTerapias retirada = terapiaService.retirarPacienteDasTerapiasFuturas(paciente);
        return new InativacaoPacienteDTO(retirada.terapiasCanceladas(), retirada.terapiasColetivasRemovido());
    }

    @Transactional
    public Paciente cadastrar(PacienteCadastroRequestDTO dto) {
        if (pessoaRepository.existsByCpf(dto.getCpf())) {
            throw new IllegalArgumentException("CPF já cadastrado.");
        }
        // Pacientes antigos sem endereço continuam válidos; o PATCH não aceita esvaziar a lista
        if (dto.getEnderecos() == null || dto.getEnderecos().isEmpty()) {
            throw new CampoInvalidoException("enderecos", "Informe ao menos um endereço.");
        }

        Paciente paciente = new Paciente();
        pessoaMapper.preencherPessoa(paciente, dto);
        paciente.setDataCadastro(LocalDate.now());
        paciente.setStatusPaciente(StatusPaciente.MATRICULADO);
        paciente.setConvenio(dto.getConvenio());
        paciente.setEscola(escolaService.buscarOpcional(dto.getIdEscola()));
        paciente.setCids(cidService.obterOuCriar(dto.getCodigosCid(), dto.getNovosCids()));
        paciente.setProntuario(prontuarioService.criar(dto.getProntuario(), paciente));

        Paciente pacienteSalvo = pacienteRepository.save(paciente);
        enderecoService.sincronizar(pacienteSalvo, dto.getEnderecos(), Moradia.sozinho());

        List<VinculoResponsavel> vinculos = new ArrayList<>();
        var cpfsResponsaveis = new LinkedHashSet<String>();

        for (int posicao = 0; posicao < dto.getResponsaveis().size(); posicao++) {
            ResponsavelCadastroRequestDTO responsavelDto = dto.getResponsaveis().get(posicao);
            if (!cpfsResponsaveis.add(responsavelDto.getCpf())) {
                throw new IllegalArgumentException("O mesmo responsável foi informado mais de uma vez");
            }

            List<Endereco> alvo = residenciaDoCadastro(pacienteSalvo, dto.getEnderecos(), responsavelDto, posicao);
            Responsavel responsavel = responsavelService.obterOuCriar(responsavelDto, pacienteSalvo, !alvo.isEmpty());

            VinculoResponsavel vinculo = new VinculoResponsavel();
            vinculo.setPaciente(pacienteSalvo);
            vinculo.setResponsavel(responsavel);
            vinculo.setGrauParentesco(responsavelDto.getGrauParentesco());
            vinculos.add(vinculo);
            responsavelService.aplicarResidencia(responsavel, vinculo, pacienteSalvo, alvo, false, Set.of());
        }

        pacienteSalvo.setResponsaveis(vinculos);
        Paciente pacienteCadastrado = pacienteRepository.save(pacienteSalvo);

        historicoPacienteService.registrar(pacienteCadastrado, TipoEventoPaciente.CADASTRO, "Paciente cadastrado.");
        for (VinculoResponsavel vinculo : vinculos) {
            registrarVinculo(pacienteCadastrado, vinculo.getResponsavel().getNome(), vinculo.getGrauParentesco());
        }
        return pacienteCadastrado;
    }

    // Retrato do paciente antes e depois de um PATCH, sem guardar valores no histórico:
    // só os nomes dos campos alterados vão para a descrição
    private record DadosParaHistorico(
            StatusCadastro statusCadastro,
            StatusPaciente statusPaciente,
            Map<String, Object> campos,
            Set<EnderecoDTO> enderecos,
            Map<Long, VinculoParaHistorico> responsaveis) {

        static DadosParaHistorico de(Paciente paciente, PessoaMapper pessoaMapper) {
            // A ordem é a usada na descrição. CIDs e prontuário aparecem só como "informações clínicas"
            Map<String, Object> campos = new LinkedHashMap<>();
            campos.put("nome", paciente.getNome());
            campos.put("celular", paciente.getCelular());
            campos.put("CNS", paciente.getNumCNS());
            campos.put("estado civil", paciente.getEstadoCivil());
            campos.put("convênio", paciente.getConvenio());
            campos.put("escola", paciente.getEscola() == null ? null : paciente.getEscola().getIdEscola());
            Prontuario prontuario = paciente.getProntuario();
            campos.put("informações clínicas", Arrays.asList(
                    paciente.getCids() == null ? Set.of()
                            : paciente.getCids().stream().map(Cid::getCodigoCID).collect(Collectors.toSet()),
                    prontuario == null ? null : Arrays.asList(
                            prontuario.getAlergias(), prontuario.getComorbidade(), prontuario.getMobilidade())));

            Map<Long, VinculoParaHistorico> responsaveis = new LinkedHashMap<>();
            if (paciente.getResponsaveis() != null) {
                paciente.getResponsaveis().forEach(vinculo -> responsaveis.put(
                        vinculo.getResponsavel().getIdCadastro(),
                        new VinculoParaHistorico(vinculo.getResponsavel().getNome(), vinculo.getGrauParentesco(),
                                vinculo.isResideComPaciente())));
            }
            return new DadosParaHistorico(
                    paciente.getStatusCadastro(), paciente.getStatusPaciente(), campos,
                    new HashSet<>(pessoaMapper.toPessoaResponseDTO(paciente).enderecos()), responsaveis);
        }

        List<String> camposAlterados(DadosParaHistorico depois) {
            List<String> alterados = new ArrayList<>();
            campos.forEach((campo, valor) -> {
                if (!Objects.equals(valor, depois.campos().get(campo))) {
                    alterados.add(campo);
                }
            });
            responsaveis.forEach((idResponsavel, vinculo) -> {
                VinculoParaHistorico atual = depois.responsaveis().get(idResponsavel);
                if (atual != null && atual.grau() != vinculo.grau()) {
                    alterados.add("parentesco de " + vinculo.nome());
                }
                if (atual != null && atual.reside() != vinculo.reside()) {
                    alterados.add("residência de " + vinculo.nome());
                }
            });
            return alterados;
        }
    }

    private record VinculoParaHistorico(String nome, GrauParentesco grau, boolean reside) {
    }

    private void atualizarProntuario(Paciente paciente, ProntuarioPatchRequestDTO dto) {
        if (dto == null) {
            paciente.setProntuario(null);
            return;
        }

        Prontuario prontuario = paciente.getProntuario();
        if (prontuario == null) {
            prontuario = new Prontuario();
            prontuario.setPaciente(paciente);
            paciente.setProntuario(prontuario);
        }

        if (dto.hasField("alergias")) {
            prontuario.setAlergias(dto.getAlergias());
        }
        if (dto.hasField("comorbidade")) {
            prontuario.setComorbidade(dto.getComorbidade());
        }
        if (dto.hasField("mobilidade")) {
            prontuario.setMobilidade(dto.getMobilidade());
        }
    }

    private List<Responsavel> substituirResponsaveis(
            Paciente paciente,
            List<ResponsavelVinculoPatchDTO> responsaveisDTO) {
        if (responsaveisDTO == null || responsaveisDTO.isEmpty()) {
            throw new IllegalArgumentException(
                    "O paciente deve possuir ao menos um responsável.");
        }

        var ids = responsaveisDTO.stream()
                .map(ResponsavelVinculoPatchDTO::idResponsavel)
                .toList();
        if (ids.stream().distinct().count() != ids.size()) {
            throw new IllegalArgumentException(
                    "O mesmo responsável foi informado mais de uma vez.");
        }

        Map<Long, VinculoResponsavel> vinculosExistentes = new HashMap<>();
        if (paciente.getResponsaveis() != null) {
            paciente.getResponsaveis().forEach(vinculo ->
                    vinculosExistentes.put(
                            vinculo.getResponsavel().getIdCadastro(), vinculo));
        }

        List<VinculoResponsavel> vinculosAtualizados = new ArrayList<>();
        for (ResponsavelVinculoPatchDTO dto : responsaveisDTO) {
            VinculoResponsavel vinculo = vinculosExistentes.remove(dto.idResponsavel());
            if (vinculo == null) {
                Responsavel responsavel = responsavelRepository.findById(dto.idResponsavel())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Responsável não encontrado com ID: " + dto.idResponsavel()));
                vinculo = new VinculoResponsavel();
                vinculo.setPaciente(paciente);
                vinculo.setResponsavel(responsavel);
            }
            vinculo.setGrauParentesco(dto.grauParentesco());
            vinculosAtualizados.add(vinculo);
        }

        if (paciente.getResponsaveis() == null) {
            paciente.setResponsaveis(vinculosAtualizados);
            return List.of();
        }
        List<Responsavel> desvinculados = vinculosExistentes.values().stream()
                .map(VinculoResponsavel::getResponsavel)
                .toList();
        paciente.getResponsaveis().removeAll(vinculosExistentes.values());
        for (VinculoResponsavel vinculo : vinculosAtualizados) {
            if (!paciente.getResponsaveis().contains(vinculo)) {
                paciente.getResponsaveis().add(vinculo);
            }
        }
        return desvinculados;
    }

}
