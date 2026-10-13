package com.fatec.gisa.dtos.terapia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import com.fatec.gisa.enums.StatusTerapia;

// Filtro das listagens de terapias, já sem valores nulos: o PostgreSQL não infere o tipo de
// "? is null", então filtros ausentes viram todos os status, um período amplo e o ID 0 (nenhum)
public record FiltroTerapiaDTO(
        Collection<StatusTerapia> status,
        LocalDateTime inicio,
        // Exclusivo: início do dia seguinte ao último dia pedido
        LocalDateTime fim,
        Long idPaciente,
        Long idEspecialista) {

    public static final long SEM_ID = 0L;
    private static final LocalDateTime SEM_INICIO = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime SEM_FIM = LocalDateTime.of(9999, 1, 1, 0, 0);

    public static FiltroTerapiaDTO semFiltro() {
        return de(null, null, null, null, null, null);
    }

    // "data" é um dia; "inicio" e "fim" formam um intervalo com os dois dias incluídos
    public static FiltroTerapiaDTO de(List<StatusTerapia> status, LocalDate data, LocalDate inicio,
            LocalDate fim, Long idPaciente, Long idEspecialista) {
        if (data != null && (inicio != null || fim != null)) {
            throw new IllegalArgumentException(
                    "Use 'data' ou o intervalo 'inicio'/'fim', não os dois juntos.");
        }
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data final deve ser igual ou posterior à data inicial.");
        }
        LocalDate primeiroDia = data != null ? data : inicio;
        LocalDate ultimoDia = data != null ? data : fim;

        return new FiltroTerapiaDTO(
                status == null || status.isEmpty() ? EnumSet.allOf(StatusTerapia.class) : status,
                primeiroDia == null ? SEM_INICIO : primeiroDia.atStartOfDay(),
                ultimoDia == null ? SEM_FIM : ultimoDia.plusDays(1).atStartOfDay(),
                id(idPaciente, "idPaciente"),
                id(idEspecialista, "idEspecialista"));
    }

    public FiltroTerapiaDTO comEspecialista(Long idEspecialista) {
        return new FiltroTerapiaDTO(status, inicio, fim, idPaciente, idEspecialista);
    }

    private static Long id(Long valor, String parametro) {
        if (valor == null) {
            return SEM_ID;
        }
        if (valor <= 0) {
            throw new IllegalArgumentException("Valor inválido para '" + parametro + "': " + valor);
        }
        return valor;
    }
}
