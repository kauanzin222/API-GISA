package com.fatec.gisa.services.profissional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.FiltroPessoaDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalCadastroRequestDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalPatchRequestDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResponseDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResumoDTO;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Profissional;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.enums.TipoCadastro;
import com.fatec.gisa.exceptions.ConflitoException;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.repositories.PessoaRepository;
import com.fatec.gisa.repositories.profissional.CargoRepository;
import com.fatec.gisa.repositories.profissional.ProfissionalRepository;
import com.fatec.gisa.repositories.usuario.PerfilRepository;
import com.fatec.gisa.services.PessoaService;
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
    private final PessoaService pessoaService;

    @Transactional
    public Profissional criarProfissional(ProfissionalCadastroRequestDTO dto) {
        validarDadosUnicos(dto);

        Profissional profissional = new Profissional();

        Cargo cargo = cargoRepository.findById(dto.getIdCargo())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cargo não encontrado com ID: " + dto.getIdCargo()));

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

    // tipos e statusCadastro vazios/nulos não restringem a busca
    @Transactional(readOnly = true)
    public Page<ProfissionalResumoDTO> listarPaginado(FiltroPessoaDTO filtro, List<TipoCadastro> tipos,
            StatusCadastro statusCadastro, Pageable pageable) {
        Set<TipoCadastro> tiposFiltrados = tipos == null || tipos.isEmpty()
                ? EnumSet.of(TipoCadastro.PROFISSIONAL, TipoCadastro.ESPECIALISTA, TipoCadastro.ESPECIALISTA_PJ)
                : EnumSet.copyOf(tipos);
        // PESSOA existe só para o usuário logado (administrador inicial), que não é profissional
        if (tiposFiltrados.contains(TipoCadastro.PESSOA)) {
            throw new IllegalArgumentException("Valor inválido para 'tipo': " + TipoCadastro.PESSOA);
        }
        Set<StatusCadastro> status = statusCadastro == null
                ? EnumSet.allOf(StatusCadastro.class)
                : EnumSet.of(statusCadastro);

        return profissionalRepository.buscar(filtro.cpf(), filtro.nome(), status,
                        tiposFiltrados.contains(TipoCadastro.PROFISSIONAL),
                        tiposFiltrados.contains(TipoCadastro.ESPECIALISTA),
                        tiposFiltrados.contains(TipoCadastro.ESPECIALISTA_PJ),
                        pageable)
                .map(profissionalMapper::toResumoDTO);
    }

    @Transactional(readOnly = true)
    public ProfissionalResponseDTO buscarDetalhes(Long idCadastro) {
        return profissionalRepository.findById(idCadastro)
                .map(profissionalMapper::toProfissionalResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profissional não encontrado com ID: " + idCadastro));
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

        pessoaService.aplicarAlteracoes(profissional, dto);

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
            // ID enviado no corpo: inexistente torna o pedido inválido (400), não a rota (404)
            Cargo cargo = cargoRepository.findById(dto.getIdCargo())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Cargo não encontrado com ID: " + dto.getIdCargo()));
            profissional.setCargo(cargo);
        }
        if (dto.hasField("idPerfil")) {
            if (dto.getIdPerfil() == null) {
                throw new IllegalArgumentException("O perfil de acesso não pode ser nulo.");
            }
            if (profissional.getUsuario() == null) {
                throw new ConflitoException(
                        "Não é possível alterar o perfil: o profissional não possui usuário.");
            }
            profissional.getUsuario().setPerfil(perfilRepository.findById(dto.getIdPerfil())
                    .orElseThrow(() -> new IllegalArgumentException(
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