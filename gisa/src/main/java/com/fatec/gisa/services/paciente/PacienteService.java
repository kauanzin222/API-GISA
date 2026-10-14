package com.fatec.gisa.services.paciente;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.EnderecoDTO;
import com.fatec.gisa.dtos.FiltroPessoaDTO;
import com.fatec.gisa.dtos.paciente.request.PacientePatchRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ProntuarioPatchRequestDTO;
import com.fatec.gisa.dtos.paciente.request.PacienteCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelVinculoPatchDTO;
import com.fatec.gisa.dtos.paciente.response.CidResponseDTO;
import com.fatec.gisa.dtos.paciente.response.CidResumoDTO;
import com.fatec.gisa.dtos.paciente.response.EscolaResponseDTO;
import com.fatec.gisa.dtos.paciente.response.PacienteDetalheDTO;
import com.fatec.gisa.dtos.paciente.response.PacienteResumoDTO;
import com.fatec.gisa.dtos.paciente.response.ProntuarioResponseDTO;
import com.fatec.gisa.dtos.paciente.response.ResponsavelResumoDTO;
import com.fatec.gisa.dtos.paciente.response.ResponsavelVinculoResponseDTO;
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
import com.fatec.gisa.repositories.paciente.PacienteRepository;
import com.fatec.gisa.repositories.paciente.ResponsavelRepository;
import com.fatec.gisa.repositories.paciente.VinculoResponsavelRepository;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.services.PessoaMapper;
import com.fatec.gisa.services.PessoaService;
import com.fatec.gisa.services.endereco.EnderecoService;

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

    // idEscola nulo não restringe; os pacientes de uma escola aparecem por este filtro
    @Transactional(readOnly = true)
    public Page<PacienteResumoDTO> listarPaginado(FiltroPessoaDTO filtro, Long idEscola, Pageable pageable) {
        if (idEscola != null && idEscola <= 0) {
            throw new IllegalArgumentException("Valor inválido para 'idEscola': " + idEscola);
        }
        Page<Paciente> pagina = pacienteRepository.buscar(
                filtro.cpf(), filtro.nome(), idEscola == null ? 0L : idEscola, pageable);
        List<Long> pacienteIds = pagina.getContent().stream()
                .map(Paciente::getIdCadastro)
                .toList();

        Map<Long, List<CidResumoDTO>> cidsPorPaciente = new HashMap<>();
        Map<Long, List<ResponsavelResumoDTO>> responsaveisPorPaciente = new HashMap<>();

        if (!pacienteIds.isEmpty()) {
            pacienteRepository.buscarComCidsPorIds(pacienteIds).forEach(paciente -> {
                List<CidResumoDTO> cids = paciente.getCids().stream()
                        .sorted(Comparator.comparing(cid -> cid.getCodigoCID()))
                        .map(cid -> new CidResumoDTO(cid.getCodigoCID(), cid.getDescricao()))
                        .toList();
                cidsPorPaciente.put(paciente.getIdCadastro(), cids);
            });

            vinculoResponsavelRepository.buscarComResponsaveisPorPacienteIds(pacienteIds).forEach(vinculo -> {
                Responsavel responsavel = vinculo.getResponsavel();
                ResponsavelResumoDTO resumo = new ResponsavelResumoDTO(
                        responsavel.getNome(), responsavel.getCelular(), vinculo.getGrauParentesco());
                responsaveisPorPaciente.computeIfAbsent(vinculo.getPaciente().getIdCadastro(), id -> new ArrayList<>()).add(resumo);
            });
        }

        return pagina.map(paciente -> new PacienteResumoDTO(
                paciente.getIdCadastro(),
                paciente.getNome(),
                calcularIdade(paciente.getDataNascimento()),
                paciente.getCpf(),
                responsaveisPorPaciente.getOrDefault(paciente.getIdCadastro(), List.of()),
                cidsPorPaciente.getOrDefault(paciente.getIdCadastro(), List.of()),
                paciente.getStatusPaciente()));
    }

    @Transactional(readOnly = true)
    public PacienteDetalheDTO buscarDetalhes(Long idCadastro) {
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
                        .map(vinculo -> new ResponsavelVinculoResponseDTO(
                                pessoaMapper.toPessoaResponseDTO(vinculo.getResponsavel()),
                                vinculo.getResponsavel().getOcupacao(),
                                vinculo.getGrauParentesco()))
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
                responsaveis);
    }

    @Transactional
    public Paciente atualizar(Long idCadastro, PacientePatchRequestDTO dto) {
        Paciente paciente = pacienteRepository.findById(idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Paciente não encontrado com ID: " + idCadastro));
        // Para o histórico: só o que realmente mudou vira evento (o front costuma reenviar o formulário inteiro)
        DadosParaHistorico antes = DadosParaHistorico.de(paciente, pessoaMapper);

        pessoaService.aplicarAlteracoes(paciente, dto);

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
        if (dto.hasField("codigosCid")) {
            paciente.setCids(cidService.obterOuCriar(
                    dto.getCodigosCid() == null ? List.of() : dto.getCodigosCid(),
                    List.of()));
        }
        if (dto.hasField("prontuario")) {
            atualizarProntuario(paciente, dto.getProntuario());
        }
        List<Responsavel> responsaveisDesvinculados = List.of();
        if (dto.hasField("responsaveis")) {
            responsaveisDesvinculados = substituirResponsaveis(paciente, dto.getResponsaveis());
        }

        Paciente pacienteAtualizado = pacienteRepository.save(paciente);
        registrarAlteracoes(pacienteAtualizado, antes, DadosParaHistorico.de(pacienteAtualizado, pessoaMapper));
        if (!responsaveisDesvinculados.isEmpty()) {
            vinculoResponsavelRepository.flush();
            responsavelService.removerOrfaosSemVinculos(responsaveisDesvinculados);
        }
        return pacienteAtualizado;
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

        List<EnderecoDTO> enderecosPaciente = pessoaMapper.toPessoaResponseDTO(paciente).enderecos();
        Responsavel responsavel = responsavelService.obterOuCriar(dto, enderecosPaciente);

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

        Paciente pacienteSalvo = pacienteRepository.save(paciente);
        registrarVinculo(pacienteSalvo, responsavel.getNome(), dto.getGrauParentesco());
        return pacienteSalvo;
    }

    @Transactional
    public void inativar(Long idCadastro) {
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
    }

    @Transactional
    public Paciente cadastrar(PacienteCadastroRequestDTO dto) {
        if (pessoaRepository.existsByCpf(dto.getCpf())) {
            throw new IllegalArgumentException("CPF já cadastrado.");
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
        enderecoService.associarEnderecos(pacienteSalvo, dto.getEnderecos());

        List<VinculoResponsavel> vinculos = new ArrayList<>();
        var cpfsResponsaveis = new LinkedHashSet<String>();

        for (ResponsavelCadastroRequestDTO responsavelDto : dto.getResponsaveis()) {
            if (!cpfsResponsaveis.add(responsavelDto.getCpf())) {
                throw new IllegalArgumentException("O mesmo responsável foi informado mais de uma vez");
            }

            Responsavel responsavel = responsavelService.obterOuCriar(responsavelDto, dto.getEnderecos());

            VinculoResponsavel vinculo = new VinculoResponsavel();
            vinculo.setPaciente(pacienteSalvo);
            vinculo.setResponsavel(responsavel);
            vinculo.setGrauParentesco(responsavelDto.getGrauParentesco());
            vinculos.add(vinculo);
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
            Map<Long, VinculoParaHistorico> responsaveis) {

        static DadosParaHistorico de(Paciente paciente, PessoaMapper pessoaMapper) {
            // A ordem é a usada na descrição. CIDs e prontuário aparecem só como "informações clínicas"
            Map<String, Object> campos = new LinkedHashMap<>();
            campos.put("nome", paciente.getNome());
            campos.put("celular", paciente.getCelular());
            campos.put("CNS", paciente.getNumCNS());
            campos.put("estado civil", paciente.getEstadoCivil());
            campos.put("endereços", new HashSet<>(pessoaMapper.toPessoaResponseDTO(paciente).enderecos()));
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
                        new VinculoParaHistorico(vinculo.getResponsavel().getNome(), vinculo.getGrauParentesco())));
            }
            return new DadosParaHistorico(
                    paciente.getStatusCadastro(), paciente.getStatusPaciente(), campos, responsaveis);
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
            });
            return alterados;
        }
    }

    private record VinculoParaHistorico(String nome, GrauParentesco grau) {
    }

    private int calcularIdade(LocalDate dataNascimento) {
        return Period.between(dataNascimento, LocalDate.now()).getYears();
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
