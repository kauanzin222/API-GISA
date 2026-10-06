package com.fatec.gisa.services.profissional;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fatec.gisa.dtos.EnderecoDTO;
import com.fatec.gisa.dtos.PessoaResponseDTO;
import com.fatec.gisa.dtos.especialista.JornadaTrabalhoDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialidadeResponseDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialistaResponseDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialistaPJResponseDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalCadastroRequestDTO;
import com.fatec.gisa.dtos.profissional.response.CargoResponseDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResponseDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResumoDTO;
import com.fatec.gisa.entities.Endereco;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.especialista.EspecialistaPJ;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Profissional;
import com.fatec.gisa.services.PessoaMapper;

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

    public PessoaResponseDTO toPessoaResponseDTO(Pessoa pessoa) {
        List<EnderecoDTO> enderecos = pessoa.getEnderecos() == null
                ? List.of()
                : pessoa.getEnderecos().stream().map(this::toEnderecoDTO).toList();

        return new PessoaResponseDTO(
                pessoa.getIdCadastro(),
                pessoa.getNome(),
                pessoa.getCpf(),
                pessoa.getDataNascimento(),
                pessoa.getSexo(),
                pessoa.getCelular(),
                pessoa.getNumCNS(),
                pessoa.getEstadoCivil(),
                pessoa.getStatusCadastro(),
                enderecos);
    }

    public ProfissionalResponseDTO toProfissionalResponseDTO(Profissional profissional) {
        return new ProfissionalResponseDTO(
                toPessoaResponseDTO(profissional),
                profissional.getEmail(),
                toCargoResponseDTO(profissional.getCargo()));
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

    private EnderecoDTO toEnderecoDTO(Endereco endereco) {
        return new EnderecoDTO(
                endereco.getCep(),
                endereco.getRua(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getEstado());
    }
}
