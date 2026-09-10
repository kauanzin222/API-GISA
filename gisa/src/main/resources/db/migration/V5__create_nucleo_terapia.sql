-- 1. Tabela: Terapia
CREATE TABLE "Terapia" (
    "IDTerapia" BIGSERIAL PRIMARY KEY,
    "DataHoraInicio" TIMESTAMP NOT NULL,
    "DataHoraFim" TIMESTAMP NOT NULL,
    "Modalidade" VARCHAR(30) NOT NULL,
    "Status" VARCHAR(30) NOT NULL
);

-- 2. Tabela Associativa: Paciente_Terapia (Presença do Paciente)
CREATE TABLE "Paciente_Terapia" (
    "IDPaciente" BIGINT NOT NULL,
    "IDTerapia" BIGINT NOT NULL,
    "Presenca" VARCHAR(30) NOT NULL,
    "Justificativa" VARCHAR(255),
    PRIMARY KEY ("IDPaciente", "IDTerapia"),
    CONSTRAINT "fk_paciente_terapia_paciente" FOREIGN KEY ("IDPaciente") REFERENCES "Paciente" ("IDPaciente") ON DELETE CASCADE,
    CONSTRAINT "fk_paciente_terapia_terapia" FOREIGN KEY ("IDTerapia") REFERENCES "Terapia" ("IDTerapia") ON DELETE CASCADE
);

-- 3. Tabela Associativa: Especialista_Terapia (N:N entre Especialista e Terapia)
CREATE TABLE "Especialista_Terapia" (
    "IDTerapia" BIGINT NOT NULL,
    "IDEspecialista" BIGINT NOT NULL,
    PRIMARY KEY ("IDTerapia", "IDEspecialista"),
    CONSTRAINT "fk_especialista_terapia_terapia" FOREIGN KEY ("IDTerapia") REFERENCES "Terapia" ("IDTerapia") ON DELETE CASCADE,
    CONSTRAINT "fk_especialista_terapia_especialista" FOREIGN KEY ("IDEspecialista") REFERENCES "Especialista" ("IDEspecialista") ON DELETE CASCADE
);