package com.fatec.gisa.services.paciente;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.fatec.gisa.enums.GrauParentesco;
import com.fatec.gisa.enums.StatusPaciente;
import com.fatec.gisa.enums.StatusPresenca;

// Trechos legíveis usados nas descrições do histórico do paciente
public final class TextoHistorico {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private static final Map<GrauParentesco, String> PARENTESCOS = Map.of(
            GrauParentesco.PAI, "pai",
            GrauParentesco.MAE, "mãe",
            GrauParentesco.AVO, "avô/avó",
            GrauParentesco.TUTOR_LEGAL, "tutor legal",
            GrauParentesco.OUTRO, "outro");

    private static final Map<StatusPaciente, String> STATUS_PACIENTE = Map.of(
            StatusPaciente.MATRICULADO, "Matriculado",
            StatusPaciente.ALTA, "Alta",
            StatusPaciente.DESLIGADO, "Desligado");

    private static final Map<StatusPresenca, String> PRESENCAS = Map.of(
            StatusPresenca.PRESENTE, "compareceu",
            StatusPresenca.FALTA_JUSTIFICADA, "falta justificada",
            StatusPresenca.FALTA_INJUSTIFICADA, "falta",
            StatusPresenca.PENDENTE, "marcação desfeita (pendente)");

    private TextoHistorico() {
    }

    // Ex.: "27/10/2026, das 09:00 às 10:00"
    public static String horario(LocalDateTime inicio, LocalDateTime fim) {
        return "%s, das %s às %s".formatted(inicio.format(DATA), inicio.format(HORA), fim.format(HORA));
    }

    // Ex.: "Ana", "Ana e Carlos", "Ana, Bia e Carlos"
    public static String lista(List<String> itens) {
        if (itens.size() <= 1) {
            return String.join("", itens);
        }
        return String.join(", ", itens.subList(0, itens.size() - 1)) + " e " + itens.get(itens.size() - 1);
    }

    public static String parentesco(GrauParentesco grau) {
        return grau == null ? "sem parentesco informado" : PARENTESCOS.get(grau);
    }

    public static String statusPaciente(StatusPaciente status) {
        return status == null ? "sem status" : STATUS_PACIENTE.get(status);
    }

    public static String presenca(StatusPresenca presenca) {
        return PRESENCAS.get(presenca);
    }
}
