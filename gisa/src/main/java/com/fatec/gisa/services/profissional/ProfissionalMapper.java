package com.fatec.gisa.services.profissional;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fatec.gisa.dtos.PessoaResponseDTO;
import com.fatec.gisa.dtos.especialista.JornadaTrabalhoDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialidadeResponseDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialistaResponseDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialistaPJResponseDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalCadastroRequestDTO;
import com.fatec.gisa.dtos.profissional.response.CargoResponseDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResponseDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResumoDTO;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.especialista.EspecialistaPJ;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Profissional;
import com.fatec.gisa.entities.usuario.Usuario;
import com.fatec.gisa.services.PessoaMapper;
import com.fatec.gisa.dtos.usuario.response.PerfilResponseDTO;
import com.fatec.gisa.dtos.usuario.response.PermissaoResumoDTO;
import com.fatec.gisa.dtos.usuario.response.UsuarioResumoDTO;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProfissionalMapper {

    private final PessoaMapper pessoaMapper;

    public void preencherProfissional(Profissional profissional,
            ProfissionalCadastroRequestDTO dto, Cargo cargo) {
        pessoaMapper.preencherPessoa(profissional, dto);
        profissional.setEmail(dto.getEmail());
        profissional.setCargo(cargo);
    }

    public ProfissionalResumoDTO toResumoDTO(Profissional profissional) {
        String registroConselho = (profissional instanceof Especialista especialista)
                ? especialista.getRegistroConselho()
                : null;

        String nomeCargo = (profissional.getCargo() != null)
                ? profissional.getCargo().getNome()
                : null;

        String status = (profissional.getStatusCadastro() != null)
                ? profissional.getStatusCadastro().name()
                : null;

        return new ProfissionalResumoDTO(
                profissional.getIdCadastro(),
                profissional.getNome(),
                nomeCargo,
                registroConselho,
                status);
    }

    public ProfissionalResponseDTO toProfissionalResponseDTO(Profissional profissional) {
        return new ProfissionalResponseDTO(
                pessoaMapper.toPessoaResponseDTO(profissional),
                profissional.getEmail(),
                toCargoResponseDTO(profissional.getCargo()),
                toUsuarioResumoDTO(profissional.getUsuario()));
    }

    public UsuarioResumoDTO toUsuarioResumoDTO(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioResumoDTO(
                usuario.getIdCadastro(),
                usuario.getPerfil() == null
                        ? null
                        : new PerfilResponseDTO(
                                usuario.getPerfil().getIdPerfil(),
                                usuario.getPerfil().getNome()),
                usuario.getPerfil() == null || usuario.getPerfil().getPermissoes() == null
                        ? List.of()
                        : usuario.getPerfil().getPermissoes().stream()
                                .map(permissao -> new PermissaoResumoDTO(
                                        permissao.getIdPermissao(),
                                        permissao.getNome(),
                                        permissao.getDescricao()))
                                .toList());
    }

    public EspecialistaResponseDTO toEspecialistaResponseDTO(Especialista especialista) {
        List<EspecialidadeResponseDTO> especialidades = especialista.getEspecialidades() == null
                ? List.of()
                : especialista.getEspecialidades().stream()
                        .map(especialidade -> new EspecialidadeResponseDTO(
                                especialidade.getIdEspecialidade(),
                                especialidade.getNome(),
                                especialidade.getDescricao()))
                        .toList();
        List<JornadaTrabalhoDTO> jornadas = especialista.getJornadaTrabalho() == null
                ? List.of()
                : especialista.getJornadaTrabalho().stream()
                        .map(jornada -> new JornadaTrabalhoDTO(
                                jornada.getIdJornada(),
                                jornada.getDiaSemana(),
                                jornada.getHoraInicio(),
                                jornada.getHoraTermino()))
                        .toList();

        return new EspecialistaResponseDTO(
                toProfissionalResponseDTO(especialista),
                especialista.getRegistroConselho(),
                especialidades,
                jornadas);
    }

    public EspecialistaPJResponseDTO toEspecialistaPJResponseDTO(EspecialistaPJ especialistaPJ) {
        return new EspecialistaPJResponseDTO(
                toEspecialistaResponseDTO(especialistaPJ),
                especialistaPJ.getCnpj(),
                especialistaPJ.getRazaoSocial(),
                especialistaPJ.getNomeFantasia(),
                especialistaPJ.getInscricaoEstadual());
    }

    private CargoResponseDTO toCargoResponseDTO(Cargo cargo) {
        if (cargo == null) {
            return null;
        }
        return new CargoResponseDTO(
                cargo.getIdCargo(),
                cargo.getNome(),
                cargo.getCbo() == null ? null : cargo.getCbo().getCodigoCBO(),
                cargo.getCbo() == null ? null : cargo.getCbo().getTituloCBO());
    }

}
