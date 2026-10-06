package com.fatec.gisa.dtos.especialista.response;

public record EspecialistaPJResponseDTO(
        EspecialistaResponseDTO especialista,
        String cnpj,
        String razaoSocial,
        String nomeFantasia,
        String inscricaoEstadual) {
}