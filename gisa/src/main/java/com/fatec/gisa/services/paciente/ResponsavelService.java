package com.fatec.gisa.services.paciente;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.FiltroPessoaDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelPatchRequestDTO;
import com.fatec.gisa.dtos.paciente.response.ResponsavelDetalheDTO;
import com.fatec.gisa.dtos.paciente.response.ResponsavelListaDTO;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.entities.paciente.Responsavel;
import com.fatec.gisa.entities.paciente.VinculoResponsavel;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.enums.TipoEventoPaciente;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.repositories.PessoaRepository;
import com.fatec.gisa.repositories.paciente.ResponsavelRepository;
import com.fatec.gisa.repositories.paciente.VinculoResponsavelRepository;
import com.fatec.gisa.repositories.usuario.UsuarioRepository;
import com.fatec.gisa.services.PessoaMapper;
import com.fatec.gisa.services.PessoaService;
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
    private final PessoaService pessoaService;
    private final HistoricoPacienteService historicoPacienteService;

    // Responsáveis já cadastrados são reaproveitados sem alterar seus dados. Um responsável novo que não
    // reside com o paciente recebe os endereços enviados para ele; o que reside recebe os do paciente
    // depois, em passarAResidir
    @Transactional
    public Responsavel obterOuCriar(ResponsavelCadastroRequestDTO dto) {
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
        if (Boolean.TRUE.equals(dto.getNaoResideComPaciente())) {
            enderecoService.associarEnderecos(responsavelSalvo, dto.getEnderecos());
        }
        return responsavelSalvo;
    }

    // Responsável que passa a residir com o paciente (vínculo novo ou marcado agora): sem endereço,
    // recebe os do paciente; com outro endereço, é recusado (ele só muda no cadastro do responsável)
    @Transactional
    public void passarAResidir(Responsavel responsavel, Paciente paciente) {
        if (enderecoService.mesmosEnderecos(responsavel, paciente)) {
            return;
        }
        if (enderecoService.temEndereco(responsavel)) {
            throw new IllegalArgumentException(("O responsável %s tem outro endereço cadastrado. Marque que ele não "
                    + "reside com o paciente ou altere o endereço no cadastro do responsável.")
                    .formatted(responsavel.getNome()));
        }
        mudarComPaciente(responsavel, paciente);
    }

    // O responsável fica com os endereços do paciente. Os outros pacientes com quem ele residia e que
    // ficaram em outro endereço passam a constar como "não reside"
    @Transactional
    public void mudarComPaciente(Responsavel responsavel, Paciente paciente) {
        enderecoService.igualarEnderecos(responsavel, paciente);
        revisarResidencias(responsavel, paciente.getIdCadastro());
    }

    private void revisarResidencias(Responsavel responsavel, Long idPacienteIgnorado) {
        if (responsavel.getDependentes() == null) {
            return;
        }
        for (VinculoResponsavel vinculo : responsavel.getDependentes()) {
            Paciente paciente = vinculo.getPaciente();
            if (!vinculo.isResideComPaciente() || paciente.getIdCadastro().equals(idPacienteIgnorado)
                    || enderecoService.mesmosEnderecos(responsavel, paciente)) {
                continue;
            }
            vinculo.setResideComPaciente(false);
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

    private void validarCpf(ResponsavelCadastroRequestDTO dto, Responsavel responsavel) {
        if (!responsavel.getCpf().equals(dto.getCpf())) {
            throw new IllegalArgumentException("O CPF informado não corresponde ao responsável selecionado");
        }
    }
}