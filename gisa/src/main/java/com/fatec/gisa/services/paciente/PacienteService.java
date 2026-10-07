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
import com.fatec.gisa.dtos.paciente.request.PacienteCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.response.CidResumoDTO;
import com.fatec.gisa.dtos.paciente.response.PacienteResumoDTO;
import com.fatec.gisa.dtos.paciente.response.ResponsavelResumoDTO;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.entities.paciente.Responsavel;
import com.fatec.gisa.entities.paciente.VinculoResponsavel;
import com.fatec.gisa.enums.StatusPaciente;
import com.fatec.gisa.repositories.PessoaRepository;
import com.fatec.gisa.repositories.paciente.PacienteRepository;
import com.fatec.gisa.repositories.paciente.VinculoResponsavelRepository;
import com.fatec.gisa.services.PessoaMapper;
import com.fatec.gisa.services.endereco.EnderecoService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final VinculoResponsavelRepository vinculoResponsavelRepository;
    private final PessoaRepository pessoaRepository;
    private final PessoaMapper pessoaMapper;
    private final EnderecoService enderecoService;
    private final EscolaService escolaService;
    private final CidService cidService;
    private final ProntuarioService prontuarioService;
    private final ResponsavelService responsavelService;

    @Transactional(readOnly = true)
    public Page<PacienteResumoDTO> listarPaginado(Pageable pageable) {
        Page<Paciente> pagina = pacienteRepository.findAll(pageable);
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

    @Transactional
    public Paciente cadastrar(PacienteCadastroRequestDTO dto) {
        if (pessoaRepository.existsByCpf(dto.getCpf())) {
            throw new IllegalArgumentException("Já existe uma pessoa cadastrada com este CPF");
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

            Responsavel responsavel = responsavelService.obterOuCriar(responsavelDto);
            List<EnderecoDTO> enderecos = Boolean.TRUE.equals(responsavelDto.getNaoResideComPaciente())
                    ? responsavelDto.getEnderecos()
                    : dto.getEnderecos();
            enderecoService.associarEnderecos(responsavel, enderecos);

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
}