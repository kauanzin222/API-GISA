package com.fatec.gisa.services.profissional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

import com.fatec.gisa.dtos.profissional.request.ProfissionalCadastroRequestDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalPatchRequestDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResponseDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResumoDTO;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Profissional;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.repositories.PessoaRepository;
import com.fatec.gisa.repositories.profissional.CargoRepository;
import com.fatec.gisa.repositories.profissional.ProfissionalRepository;
import com.fatec.gisa.repositories.usuario.PerfilRepository;
import com.fatec.gisa.services.endereco.EnderecoService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;
    private final PessoaRepository pessoaRepository;
    private final CargoRepository cargoRepository;
    private final PerfilRepository perfilRepository;
    private final ProfissionalMapper profissionalMapper;
    private final EnderecoService enderecoService;

    @Transactional
    public Profissional criarProfissional(ProfissionalCadastroRequestDTO dto) {
        validarDadosUnicos(dto);

        Profissional profissional = new Profissional();

        Cargo cargo = cargoRepository.findById(dto.getIdCargo())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cargo nao encontrado com ID: " + dto.getIdCargo()));

        profissionalMapper.preencherProfissional(profissional, dto, cargo);

        Profissional profissionalSalvo = profissionalRepository.save(profissional);
        enderecoService.associarEnderecos(profissionalSalvo, dto.getEnderecos());
        return profissionalSalvo;
    }

    // Usado também no cadastro de especialistas e especialistas PJ
    @Transactional(readOnly = true)
    public void validarDadosUnicos(ProfissionalCadastroRequestDTO dto) {
        if (pessoaRepository.existsByCpf(dto.getCpf())) {
            throw new IllegalArgumentException("CPF já cadastrado.");
        }
        if (profissionalRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }
    }

    @Transactional(readOnly = true)
    public Page<ProfissionalResumoDTO> listarPaginado(Pageable pageable) {
        return profissionalRepository.findAll(pageable)
                .map(profissionalMapper::toResumoDTO);
    }

    @Transactional(readOnly = true)
    public Optional<ProfissionalResponseDTO> buscarDetalhes(Long idCadastro) {
        return profissionalRepository.findById(idCadastro)
                .map(profissionalMapper::toProfissionalResponseDTO);
    }

    @Transactional(readOnly = true)
    public Profissional buscarParaConcederAcesso(Long idCadastro) {
        return profissionalRepository.findById(idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profissional não encontrado com ID: " + idCadastro));
    }

    @Transactional
    public Profissional atualizar(Long idCadastro, ProfissionalPatchRequestDTO dto) {
        Profissional profissional = profissionalRepository.findById(idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profissional não encontrado com ID: " + idCadastro));

        if (dto.hasField("nome")) {
            if (dto.getNome() == null || dto.getNome().isBlank()) {
                throw new IllegalArgumentException("O nome não pode ser vazio.");
            }
            profissional.setNome(dto.getNome());
        }
        if (dto.hasField("celular")) {
            profissional.setCelular(dto.getCelular());
        }
        if (dto.hasField("numCNS")) {
            profissional.setNumCNS(dto.getNumCNS());
        }
        if (dto.hasField("estadoCivil")) {
            if (dto.getEstadoCivil() == null) {
                throw new IllegalArgumentException("O estado civil não pode ser nulo.");
            }
            profissional.setEstadoCivil(dto.getEstadoCivil());
        }
        if (dto.hasField("statusCadastro")) {
            if (dto.getStatusCadastro() == null) {
                throw new IllegalArgumentException("O status do cadastro não pode ser nulo.");
            }
            profissional.setStatusCadastro(dto.getStatusCadastro());
        }
        if (dto.hasField("enderecos")) {
            enderecoService.substituirEnderecos(profissional, dto.getEnderecos());
        }
        if (dto.hasField("email")) {
            if (dto.getEmail() == null || dto.getEmail().isBlank()) {
                throw new IllegalArgumentException("O e-mail não pode ser vazio.");
            }
            if (profissionalRepository.existsByEmailAndIdCadastroNot(
                    dto.getEmail(), profissional.getIdCadastro())) {
                throw new IllegalArgumentException("E-mail já cadastrado.");
            }
            profissional.setEmail(dto.getEmail());
        }
        if (dto.hasField("idCargo")) {
            if (dto.getIdCargo() == null) {
                throw new IllegalArgumentException("O cargo não pode ser nulo.");
            }
            Cargo cargo = cargoRepository.findById(dto.getIdCargo())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Cargo não encontrado com ID: " + dto.getIdCargo()));
            profissional.setCargo(cargo);
        }
        if (dto.hasField("idPerfil")) {
            if (dto.getIdPerfil() == null) {
                throw new IllegalArgumentException("O perfil de acesso não pode ser nulo.");
            }
            if (profissional.getUsuario() == null) {
                throw new IllegalStateException(
                        "Não é possível alterar o perfil: o profissional não possui usuário.");
            }
            profissional.getUsuario().setPerfil(perfilRepository.findById(dto.getIdPerfil())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Perfil não encontrado com ID: " + dto.getIdPerfil())));
        }

        return profissionalRepository.save(profissional);
    }

    @Transactional
    public void inativar(Long idCadastro) {
        Profissional profissional = profissionalRepository.findById(idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profissional não encontrado com ID: " + idCadastro));
        profissional.setStatusCadastro(StatusCadastro.INATIVO);
    }
}