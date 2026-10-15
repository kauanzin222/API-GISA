package com.fatec.gisa.dtos.paciente.response;

// Resposta de DELETE /api/pacientes/{id}: o que foi feito com as terapias agendadas futuras do paciente
public record InativacaoPacienteDTO(
        // Terapias individuais (só com este paciente) canceladas com o motivo "Paciente inativado"
        int terapiasCanceladas,
        // Terapias coletivas de que o paciente foi retirado; elas continuam para os demais
        int terapiasColetivasRemovido) {
}
