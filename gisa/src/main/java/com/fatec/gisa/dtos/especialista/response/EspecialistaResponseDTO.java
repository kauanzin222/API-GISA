package com.fatec.gisa.dtos.especialista.response;

import java.time.LocalDate;
import java.util.List;

import com.fatec.gisa.dtos.especialista.EspecialidadeDTO;
import com.fatec.gisa.dtos.especialista.JornadaTrabalhoDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EspecialistaResponseDTO {

    private Long idCadastro;
    private String nome;
    private String cpf;
    private String email;
    private String telefone;
    private LocalDate dataNascimento;

    // Dados do Cargo e Conselho
    private Long idCargo;
    private String nomeCargo;
    private String registroConselho;

    // Dados PJ (populados apenas se for PJ)
    private Boolean isPJ;
    private String cnpj;
    private String razaoSocial;
    private String nomeFantasia;
    private String inscricaoEstadual;

    // Listas associadas
    private List<EspecialidadeDTO> especialidades;
    private List<JornadaTrabalhoDTO> jornadas;
}