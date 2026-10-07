package com.fatec.gisa.services.paciente;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.EnderecoDTO;
import com.fatec.gisa.dtos.paciente.request.PacienteCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelCadastroRequestDTO;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.entities.paciente.Responsavel;
import com.fatec.gisa.entities.paciente.VinculoResponsavel;
import com.fatec.gisa.enums.StatusPaciente;
import com.fatec.gisa.repositories.PessoaRepository;
import com.fatec.gisa.repositories.paciente.PacienteRepository;
import com.fatec.gisa.services.PessoaMapper;
import com.fatec.gisa.services.endereco.EnderecoService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final PessoaRepository pessoaRepository;
    private final PessoaMapper pessoaMapper;
    private final EnderecoService enderecoService;
    private final EscolaService escolaService;
    private final CidService cidService;
    private final ProntuarioService prontuarioService;
    private final ResponsavelService responsavelService;

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
}