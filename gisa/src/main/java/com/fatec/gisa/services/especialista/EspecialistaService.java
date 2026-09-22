package com.fatec.gisa.services.especialista;

import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.especialista.request.EspecialistaCadastroRequestDTO;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.repositories.especialista.EspecialistaRepository;
import com.fatec.gisa.repositories.profissional.CargoRepository;
import com.fatec.gisa.services.cadastro.CadastroMapper;
import com.fatec.gisa.services.endereco.EnderecoService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EspecialistaService {

    private final EspecialistaRepository especialistaRepository;
    private final CargoRepository cargoRepository;
    private final CadastroMapper cadastroMapper;
    private final EspecialidadeService especialidadeService;
    private final JornadaTrabalhoService jornadaTrabalhoService;
    private final EnderecoService enderecoService;

    @Transactional
    public Especialista cadastrar(EspecialistaCadastroRequestDTO dto) {
        Especialista especialista = new Especialista();
        preencherDadosEspecialista(especialista, dto);
        Especialista especialistaSalvo = especialistaRepository.save(especialista);
        enderecoService.associarEnderecos(especialistaSalvo, dto.getEnderecos());
        return especialistaSalvo;
    }

    void preencherDadosEspecialista(Especialista especialista, EspecialistaCadastroRequestDTO dto) {
        Cargo cargo = cargoRepository.findById(dto.getIdCargo())
            .orElseThrow(() -> new IllegalArgumentException(
                "Cargo nao encontrado com ID: " + dto.getIdCargo()));
        cadastroMapper.preencherProfissional(especialista, dto, cargo);
        especialista.setRegistroConselho(dto.getRegistroConselho());
        especialista.setEspecialidades(new java.util.ArrayList<>(especialidadeService.obterOuCriar(
                dto.getEspecialidadesIds(), dto.getNovasEspecialidades())));
        especialista.setJornadaTrabalho(jornadaTrabalhoService.mapear(dto.getJornadas(), especialista));
    }
}
