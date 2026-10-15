package com.fatec.gisa.services.profissional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.FiltroPessoaDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalCadastroRequestDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalPatchRequestDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResponseDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResumoDTO;
import com.fatec.gisa.entities.assistentesocial.AssistenteSocial;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Profissional;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.enums.TipoCadastro;
import com.fatec.gisa.exceptions.ConflitoException;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.repositories.PessoaRepository;
import com.fatec.gisa.repositories.assistentesocial.VinculoAssistenteSocialRepository;
import com.fatec.gisa.repositories.profissional.CargoRepository;
import com.fatec.gisa.repositories.profissional.ProfissionalRepository;
import com.fatec.gisa.services.PessoaService;
import com.fatec.gisa.services.endereco.EnderecoService;
import com.fatec.gisa.services.usuario.PermissaoUsuarioLogado;
import com.fatec.gisa.services.usuario.UsuarioService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;
    private final PessoaRepository pessoaRepository;
    private final CargoRepository cargoRepository;
    private final UsuarioService usuarioService;
    private final ProfissionalMapper profissionalMapper;
    private final EnderecoService enderecoService;
    private final PessoaService pessoaService;
    private final VinculoAssistenteSocialRepository vinculoAssistenteSocialRepository;
    private final Clock relogio;

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
        // O e-mail é o login: único entre todos os cadastros, sem diferenciar maiúsculas
        if (pessoaRepository.existsByEmailIgnoreCase(dto.getEmail())) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }
    }

    // tipos e statusCadastro vazios/nulos não restringem a busca
    @Transactional(readOnly = true)
    public Page<ProfissionalResumoDTO> listarPaginado(FiltroPessoaDTO filtro, List<TipoCadastro> tipos,
            StatusCadastro statusCadastro, Pageable pageable) {
        Set<TipoCadastro> tiposFiltrados = tipos == null || tipos.isEmpty()
                ? EnumSet.of(TipoCadastro.PROFISSIONAL, TipoCadastro.ESPECIALISTA, TipoCadastro.ESPECIALISTA_PJ,
                        TipoCadastro.ASSISTENTE_SOCIAL)
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
                        tiposFiltrados.contains(TipoCadastro.ASSISTENTE_SOCIAL),
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
        // Trocar o perfil é gerenciar usuários: EDITAR_PROFISSIONAL (exigido na rota) não basta
        if (dto.hasField("idPerfil")) {
            PermissaoUsuarioLogado.exigir("GERENCIAR_USUARIOS");
        }

        Profissional profissional = profissionalRepository.findById(idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profissional não encontrado com ID: " + idCadastro));

        boolean inativando = dto.hasField("statusCadastro")
                && dto.getStatusCadastro() == StatusCadastro.INATIVO
                && profissional.getStatusCadastro() != StatusCadastro.INATIVO;
        if (inativando) {
            antesDeInativar(profissional);
        }

        pessoaService.aplicarAlteracoes(profissional, dto);

        if (dto.hasField("email")) {
            if (dto.getEmail() == null || dto.getEmail().isBlank()) {
                throw new IllegalArgumentException("O e-mail não pode ser vazio.");
            }
            if (pessoaRepository.existsByEmailIgnoreCaseAndIdCadastroNot(
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
            usuarioService.alterarPerfil(profissional.getUsuario(), dto.getIdPerfil());
        }

        return profissionalRepository.save(profissional);
    }

    @Transactional
    public void inativar(Long idCadastro) {
        Profissional profissional = profissionalRepository.findById(idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profissional não encontrado com ID: " + idCadastro));
        if (profissional.getStatusCadastro() != StatusCadastro.INATIVO) {
            antesDeInativar(profissional);
        }
        profissional.setStatusCadastro(StatusCadastro.INATIVO);
    }

    // Vale para o DELETE e para o PATCH com statusCadastro INATIVO, de qualquer tipo de profissional:
    // não deixa o sistema sem quem gerencie perfis (409) e encerra os vínculos do assistente social
    private void antesDeInativar(Profissional profissional) {
        if (profissional.getUsuario() != null) {
            Long idUsuario = profissional.getIdCadastro();
            usuarioService.garantirGestorDePerfis(gestor -> gestor.getIdCadastro().equals(idUsuario));
        }
        if (Hibernate.unproxy(profissional) instanceof AssistenteSocial) {
            LocalDate hoje = LocalDate.now(relogio);
            vinculoAssistenteSocialRepository.buscarAtivosDoAssistenteSocial(profissional.getIdCadastro())
                    .forEach(vinculo -> vinculo.setDataFim(hoje));
        }
    }
}
