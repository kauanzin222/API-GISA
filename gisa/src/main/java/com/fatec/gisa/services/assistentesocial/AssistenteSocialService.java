package com.fatec.gisa.services.assistentesocial;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.assistentesocial.request.AssistenteSocialCadastroRequestDTO;
import com.fatec.gisa.dtos.assistentesocial.response.AssistenteSocialResponseDTO;
import com.fatec.gisa.dtos.paciente.response.PacienteBuscaDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalPatchRequestDTO;
import com.fatec.gisa.entities.assistentesocial.AssistenteSocial;
import com.fatec.gisa.entities.assistentesocial.VinculoAssistenteSocial;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.exceptions.ConflitoException;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.repositories.assistentesocial.AssistenteSocialRepository;
import com.fatec.gisa.repositories.assistentesocial.VinculoAssistenteSocialRepository;
import com.fatec.gisa.repositories.paciente.PacienteRepository;
import com.fatec.gisa.repositories.profissional.CargoRepository;
import com.fatec.gisa.services.BuscaPorIds;
import com.fatec.gisa.services.endereco.EnderecoService;
import com.fatec.gisa.services.paciente.PacienteService;
import com.fatec.gisa.services.profissional.ProfissionalMapper;
import com.fatec.gisa.services.profissional.ProfissionalService;

import lombok.RequiredArgsConstructor;

// Assistente social: profissional (mesmas regras de cadastro, PATCH, inativação e acesso) com
// registro no CRESS e pacientes vinculados. Os vínculos são editados só por vincular/desvincular,
// usados tanto pela tela do assistente social quanto pela do paciente.
@Service
@RequiredArgsConstructor
public class AssistenteSocialService {

    private final AssistenteSocialRepository assistenteSocialRepository;
    private final VinculoAssistenteSocialRepository vinculoAssistenteSocialRepository;
    private final PacienteRepository pacienteRepository;
    private final CargoRepository cargoRepository;
    private final ProfissionalService profissionalService;
    private final ProfissionalMapper profissionalMapper;
    private final EnderecoService enderecoService;
    // No fuso de Sorocaba (ver RelogioConfig): data de início e de fim dos vínculos
    private final Clock relogio;

    @Transactional
    public AssistenteSocial cadastrar(AssistenteSocialCadastroRequestDTO dto) {
        profissionalService.validarDadosUnicos(dto);
        Cargo cargo = cargoRepository.findById(dto.getIdCargo())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cargo não encontrado com ID: " + dto.getIdCargo()));
        List<Paciente> pacientes = BuscaPorIds.buscarTodos(dto.getIdsPacientes(), pacienteRepository::findAllById,
                Paciente::getIdCadastro, "Paciente(s) não encontrado(s)");

        AssistenteSocial assistenteSocial = new AssistenteSocial();
        profissionalMapper.preencherProfissional(assistenteSocial, dto, cargo);
        assistenteSocial.setRegistroConselho(dto.getRegistroConselho());
        AssistenteSocial salvo = assistenteSocialRepository.save(assistenteSocial);
        enderecoService.associarEnderecos(salvo, dto.getEnderecos());

        LocalDate hoje = LocalDate.now(relogio);
        vinculoAssistenteSocialRepository.saveAll(pacientes.stream()
                .map(paciente -> new VinculoAssistenteSocial(null, paciente, salvo, hoje, null))
                .toList());
        return salvo;
    }

    @Transactional(readOnly = true)
    public AssistenteSocialResponseDTO buscarDetalhes(Long idCadastro) {
        AssistenteSocial assistenteSocial = buscar(idCadastro);
        return new AssistenteSocialResponseDTO(
                profissionalMapper.toProfissionalResponseDTO(assistenteSocial),
                assistenteSocial.getRegistroConselho(),
                pacientesVinculados(idCadastro));
    }

    // GET /api/assistentes-sociais/me/pacientes. cpf vem do token
    @Transactional(readOnly = true)
    public List<PacienteBuscaDTO> listarPacientesDoUsuarioLogado(String cpf) {
        AssistenteSocial assistenteSocial = assistenteSocialRepository.findByCpf(cpf)
                .orElseThrow(() -> new IllegalArgumentException("O usuário logado não é um assistente social."));
        return pacientesVinculados(assistenteSocial.getIdCadastro());
    }

    // Mesmas regras do PATCH de profissional (inclusive idPerfil, reativação e inativação, que
    // encerra os vínculos). registroConselho não é alterado, como no especialista
    @Transactional
    public void atualizar(Long idCadastro, ProfissionalPatchRequestDTO dto) {
        buscar(idCadastro);
        profissionalService.atualizar(idCadastro, dto);
    }

    // Inativar encerra os vínculos ativos (ver ProfissionalService.inativar)
    @Transactional
    public void inativar(Long idCadastro) {
        buscar(idCadastro);
        profissionalService.inativar(idCadastro);
    }

    @Transactional(readOnly = true)
    public AssistenteSocial buscarParaConcederAcesso(Long idCadastro) {
        return buscar(idCadastro);
    }

    @Transactional
    public AssistenteSocialResponseDTO vincularPaciente(Long idCadastro, Long idPaciente) {
        AssistenteSocial assistenteSocial = buscar(idCadastro);
        if (assistenteSocial.getStatusCadastro() == StatusCadastro.INATIVO) {
            throw new IllegalArgumentException("Não é possível vincular pacientes a um assistente social inativo.");
        }
        // ID enviado no corpo: inexistente torna o pedido inválido (400), não a rota (404)
        Paciente paciente = pacienteRepository.findById(idPaciente)
                .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado com ID: " + idPaciente));
        if (vinculoAssistenteSocialRepository
                .existsByPaciente_IdCadastroAndAssistenteSocial_IdCadastroAndDataFimIsNull(idPaciente, idCadastro)) {
            throw new ConflitoException("O paciente já está vinculado a este assistente social.");
        }
        vinculoAssistenteSocialRepository.save(
                new VinculoAssistenteSocial(null, paciente, assistenteSocial, LocalDate.now(relogio), null));
        return buscarDetalhes(idCadastro);
    }

    // Não apaga a linha: preenche a data de fim. Revincular depois cria um vínculo novo
    @Transactional
    public void desvincularPaciente(Long idCadastro, Long idPaciente) {
        buscar(idCadastro);
        VinculoAssistenteSocial vinculo = vinculoAssistenteSocialRepository
                .findByPaciente_IdCadastroAndAssistenteSocial_IdCadastroAndDataFimIsNull(idPaciente, idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "O paciente com ID " + idPaciente + " não tem vínculo ativo com este assistente social."));
        vinculo.setDataFim(LocalDate.now(relogio));
    }

    private List<PacienteBuscaDTO> pacientesVinculados(Long idAssistenteSocial) {
        return vinculoAssistenteSocialRepository.buscarAtivosDoAssistenteSocial(idAssistenteSocial).stream()
                .map(vinculo -> PacienteService.toBuscaDTO(vinculo.getPaciente()))
                .toList();
    }

    private AssistenteSocial buscar(Long idCadastro) {
        return assistenteSocialRepository.findById(idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assistente social não encontrado com ID: " + idCadastro));
    }
}
