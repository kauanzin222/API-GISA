package com.fatec.gisa.services.paciente;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

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
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.enums.StatusPaciente;
import com.fatec.gisa.repositories.PessoaRepository;
import com.fatec.gisa.repositories.paciente.PacienteRepository;
import com.fatec.gisa.repositories.paciente.ResponsavelRepository;
import com.fatec.gisa.repositories.paciente.VinculoResponsavelRepository;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.services.PessoaMapper;
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
    private final EscolaService escolaService;
    private final CidService cidService;
    private final ProntuarioService prontuarioService;
    private final ResponsavelService responsavelService;

    @Transactional(readOnly = true)
    public Page<PacienteResumoDTO> listarPaginado(FiltroPessoaDTO filtro, Pageable pageable) {
        Page<Paciente> pagina = pacienteRepository.buscarPorCpfENome(filtro.cpf(), filtro.nome(), pageable);
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

        atualizarPessoa(paciente, dto);

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
        if (dto.hasField("enderecos")) {
            enderecoService.substituirEnderecos(paciente, dto.getEnderecos());
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
        if (!responsaveisDesvinculados.isEmpty()) {
            vinculoResponsavelRepository.flush();
            responsavelService.removerOrfaosSemVinculos(responsaveisDesvinculados);
        }
        return pacienteAtualizado;
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

        return pacienteRepository.save(paciente);
    }

    @Transactional
    public void inativar(Long idCadastro) {
        Paciente paciente = pacienteRepository.findById(idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Paciente não encontrado com ID: " + idCadastro));

        paciente.setStatusCadastro(StatusCadastro.INATIVO);
        if (paciente.getStatusPaciente() != StatusPaciente.ALTA) {
            paciente.setStatusPaciente(StatusPaciente.DESLIGADO);
        }
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
        return pacienteRepository.save(pacienteSalvo);
    }

    private int calcularIdade(LocalDate dataNascimento) {
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }

    private void atualizarPessoa(Pessoa pessoa, PacientePatchRequestDTO dto) {
        if (dto.hasField("nome")) {
            if (dto.getNome() == null || dto.getNome().isBlank()) {
                throw new IllegalArgumentException("O nome não pode ser vazio.");
            }
            pessoa.setNome(dto.getNome());
        }
        if (dto.hasField("celular")) {
            pessoa.setCelular(dto.getCelular());
        }
        if (dto.hasField("numCNS")) {
            pessoa.setNumCNS(dto.getNumCNS());
        }
        if (dto.hasField("estadoCivil")) {
            if (dto.getEstadoCivil() == null) {
                throw new IllegalArgumentException("O estado civil não pode ser nulo.");
            }
            pessoa.setEstadoCivil(dto.getEstadoCivil());
        }
        if (dto.hasField("statusCadastro")) {
            if (dto.getStatusCadastro() == null) {
                throw new IllegalArgumentException("O status do cadastro não pode ser nulo.");
            }
            pessoa.setStatusCadastro(dto.getStatusCadastro());
        }
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
                        .orElseThrow(() -> new ResourceNotFoundException(
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