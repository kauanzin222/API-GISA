package com.fatec.gisa.services.paciente;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.EnderecoDTO;
import com.fatec.gisa.dtos.PessoaPatchRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelPatchRequestDTO;
import com.fatec.gisa.dtos.paciente.response.ResponsavelDetalheDTO;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.paciente.Responsavel;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.repositories.PessoaRepository;
import com.fatec.gisa.repositories.paciente.ResponsavelRepository;
import com.fatec.gisa.repositories.paciente.VinculoResponsavelRepository;
import com.fatec.gisa.repositories.usuario.UsuarioRepository;
import com.fatec.gisa.services.PessoaMapper;
import com.fatec.gisa.services.endereco.EnderecoService;

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

    @Transactional
    // Responsáveis já cadastrados são reaproveitados sem alterar seus dados (inclusive endereços).
    // Um responsável novo recebe os endereços do paciente, salvo quando não reside com ele.
    public Responsavel obterOuCriar(ResponsavelCadastroRequestDTO dto, List<EnderecoDTO> enderecosPaciente) {
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

        List<EnderecoDTO> enderecos = Boolean.TRUE.equals(dto.getNaoResideComPaciente())
                ? dto.getEnderecos()
                : enderecosPaciente;
        enderecoService.associarEnderecos(responsavelSalvo, enderecos);
        return responsavelSalvo;
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
        atualizarPessoa(responsavel, dto);

        if (dto.hasField("ocupacao")) {
            responsavel.setOcupacao(dto.getOcupacao());
        }
        if (dto.hasField("enderecos")) {
            enderecoService.substituirEnderecos(responsavel, dto.getEnderecos());
        }

        responsavelRepository.save(responsavel);
        return new ResponsavelDetalheDTO(
                pessoaMapper.toPessoaResponseDTO(responsavel),
                responsavel.getOcupacao());
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

            enderecoService.substituirEnderecos(responsavel, List.of());
            responsavelRepository.delete(responsavel);
        }
    }

    private Responsavel buscarResponsavel(Long idResponsavel) {
        return responsavelRepository.findById(idResponsavel)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Responsável não encontrado com ID: " + idResponsavel));
    }

    private void atualizarPessoa(Pessoa pessoa, PessoaPatchRequestDTO dto) {
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

    private void validarCpf(ResponsavelCadastroRequestDTO dto, Responsavel responsavel) {
        if (!responsavel.getCpf().equals(dto.getCpf())) {
            throw new IllegalArgumentException("O CPF informado não corresponde ao responsável selecionado");
        }
    }
}