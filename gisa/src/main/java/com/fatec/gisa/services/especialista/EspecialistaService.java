package com.fatec.gisa.services.especialista;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.especialista.request.EspecialistaCadastroRequestDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialistaPatchRequestDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialistaResponseDTO;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.especialista.Especialidade;
import com.fatec.gisa.entities.especialista.JornadaTrabalho;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.repositories.especialista.EspecialistaRepository;
import com.fatec.gisa.repositories.especialista.EspecialistaPJRepository;
import com.fatec.gisa.repositories.profissional.CargoRepository;
import com.fatec.gisa.services.endereco.EnderecoService;
import com.fatec.gisa.services.profissional.ProfissionalMapper;
import com.fatec.gisa.services.profissional.ProfissionalService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EspecialistaService {

    private final EspecialistaRepository especialistaRepository;
    private final EspecialistaPJRepository especialistaPJRepository;
    private final CargoRepository cargoRepository;
    private final ProfissionalMapper profissionalMapper;
    private final EspecialidadeService especialidadeService;
    private final JornadaTrabalhoService jornadaTrabalhoService;
    private final EnderecoService enderecoService;
    private final ProfissionalService profissionalService;

    @Transactional
    public Especialista cadastrar(EspecialistaCadastroRequestDTO dto) {
        Especialista especialista = new Especialista();
        preencherDadosEspecialista(especialista, dto);
        Especialista especialistaSalvo = especialistaRepository.save(especialista);
        enderecoService.associarEnderecos(especialistaSalvo, dto.getEnderecos());
        return especialistaSalvo;
    }

    @Transactional(readOnly = true)
    public EspecialistaResponseDTO buscarDetalhes(Long idCadastro) {
        validarCadastroNaoPJ(idCadastro);
        return especialistaRepository.findById(idCadastro)
                .map(profissionalMapper::toEspecialistaResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Especialista não encontrado com ID: " + idCadastro));
    }

    @Transactional
    public Especialista atualizar(Long idCadastro, EspecialistaPatchRequestDTO dto) {
        validarCadastroNaoPJ(idCadastro);
        return atualizarDados(idCadastro, dto);
    }

    @Transactional
    public Especialista atualizarParaPJ(Long idCadastro, EspecialistaPatchRequestDTO dto) {
        return atualizarDados(idCadastro, dto);
    }

    private Especialista atualizarDados(Long idCadastro, EspecialistaPatchRequestDTO dto) {
        profissionalService.atualizar(idCadastro, dto);
        Especialista especialista = especialistaRepository.findById(idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Especialista não encontrado com ID: " + idCadastro));

        if (dto.hasField("especialidadesIds") || dto.hasField("novasEspecialidades")) {
            List<Long> ids = dto.hasField("especialidadesIds")
                    ? dto.getEspecialidadesIds()
                    : especialista.getEspecialidades().stream()
                            .map(Especialidade::getIdEspecialidade)
                            .toList();
            especialista.setEspecialidades(new java.util.ArrayList<>(
                    especialidadeService.obterOuCriar(
                            ids,
                            dto.hasField("novasEspecialidades")
                                    ? dto.getNovasEspecialidades()
                                    : List.of())));
        }
        if (dto.hasField("jornadas")) {
            if (dto.getJornadas() == null || dto.getJornadas().isEmpty()) {
                throw new IllegalArgumentException(
                        "O especialista deve possuir ao menos uma jornada de trabalho.");
            }
            List<JornadaTrabalho> jornadasAtualizadas =
                    jornadaTrabalhoService.mapear(dto.getJornadas(), especialista);
            if (especialista.getJornadaTrabalho() == null) {
                especialista.setJornadaTrabalho(new java.util.ArrayList<>());
            } else {
                especialista.getJornadaTrabalho().clear();
            }
            especialista.getJornadaTrabalho().addAll(jornadasAtualizadas);
        }

        return especialistaRepository.save(especialista);
    }

    @Transactional
    public void inativar(Long idCadastro) {
        validarCadastroNaoPJ(idCadastro);
        if (!especialistaRepository.existsById(idCadastro)) {
            throw new ResourceNotFoundException(
                    "Especialista não encontrado com ID: " + idCadastro);
        }
        profissionalService.inativar(idCadastro);
    }

    @Transactional
    public void inativarParaPJ(Long idCadastro) {
        if (!especialistaRepository.existsById(idCadastro)) {
            throw new ResourceNotFoundException(
                    "Especialista não encontrado com ID: " + idCadastro);
        }
        profissionalService.inativar(idCadastro);
    }

    @Transactional(readOnly = true)
    public Especialista buscarParaConcederAcesso(Long idCadastro) {
        validarCadastroNaoPJ(idCadastro);
        return especialistaRepository.findById(idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Especialista não encontrado com ID: " + idCadastro));
    }

    private void validarCadastroNaoPJ(Long idCadastro) {
        if (especialistaPJRepository.existsById(idCadastro)) {
            throw new IllegalArgumentException(
                    "Este cadastro é de um especialista PJ. Use /api/especialistas/pj/{idCadastro}.");
        }
    }

    void preencherDadosEspecialista(Especialista especialista, EspecialistaCadastroRequestDTO dto) {
        profissionalService.validarDadosUnicos(dto);

        Cargo cargo = cargoRepository.findById(dto.getIdCargo())
            .orElseThrow(() -> new IllegalArgumentException(
                "Cargo não encontrado com ID: " + dto.getIdCargo()));
        profissionalMapper.preencherProfissional(especialista, dto, cargo);
        especialista.setRegistroConselho(dto.getRegistroConselho());
        especialista.setEspecialidades(new java.util.ArrayList<>(especialidadeService.obterOuCriar(
                dto.getEspecialidadesIds(), dto.getNovasEspecialidades())));
        especialista.setJornadaTrabalho(jornadaTrabalhoService.mapear(dto.getJornadas(), especialista));
    }
}
