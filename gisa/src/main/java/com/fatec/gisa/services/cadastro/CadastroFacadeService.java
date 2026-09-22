package com.fatec.gisa.services.cadastro;

import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.especialista.request.EspecialistaCadastroRequestDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialistaPJCadastroRequestDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalCadastroRequestDTO;
import com.fatec.gisa.dtos.usuario.request.UsuarioRequestDTO;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.especialista.EspecialistaPJ;
import com.fatec.gisa.entities.profissional.Profissional;
import com.fatec.gisa.services.especialista.EspecialistaPJService;
import com.fatec.gisa.services.especialista.EspecialistaService;
import com.fatec.gisa.services.profissional.ProfissionalService;
import com.fatec.gisa.services.usuario.UsuarioService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CadastroFacadeService {

    private final EspecialistaService especialistaService;
    private final EspecialistaPJService especialistaPJService;
    private final UsuarioService usuarioService;
    private final ProfissionalService profissionalService;

    @Transactional
    public Especialista cadastrarEspecialista(EspecialistaCadastroRequestDTO dto) {
        return especialistaService.cadastrar(dto);
    }

    @Transactional
    public Especialista cadastrarEspecialistaComUsuario(EspecialistaCadastroRequestDTO dto,
            UsuarioRequestDTO usuarioDto) {
        Especialista especialista = especialistaService.cadastrar(dto);
        usuarioService.criarUsuarioParaPessoa(especialista, usuarioDto);
        return especialista;
    }

    @Transactional
    public EspecialistaPJ cadastrarEspecialistaPJ(EspecialistaPJCadastroRequestDTO dto) {
        return especialistaPJService.cadastrar(dto);
    }

    @Transactional
    public EspecialistaPJ cadastrarEspecialistaPJComUsuario(EspecialistaPJCadastroRequestDTO dto,
            UsuarioRequestDTO usuarioDto) {
        EspecialistaPJ especialistaPJ = especialistaPJService.cadastrar(dto);
        usuarioService.criarUsuarioParaPessoa(especialistaPJ, usuarioDto);
        return especialistaPJ;
    }

    @Transactional
    public void criarUsuario(Pessoa pessoa, UsuarioRequestDTO dto) {
        usuarioService.criarUsuarioParaPessoa(pessoa, dto);
    }

    @Transactional
    public Profissional cadastrarProfissional(ProfissionalCadastroRequestDTO dto) {
        return profissionalService.criarProfissional(dto);
    }
}
