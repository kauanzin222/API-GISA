-- 1. Tabela: Especialidade
CREATE TABLE "Especialidade" (
    "IDEspecialidade" BIGSERIAL PRIMARY KEY,
    "Nome" VARCHAR(100) NOT NULL,
    "Descricao" VARCHAR(255)
);

-- 2. Tabela: Especialista (Especialização de Profissional)
CREATE TABLE "Especialista" (
    "IDEspecialista" BIGINT PRIMARY KEY,
    "RegistroConselho" VARCHAR(50) NOT NULL,
    CONSTRAINT "fk_especialista_profissional" FOREIGN KEY ("IDEspecialista") REFERENCES "Profissional" ("IDProfissional") ON DELETE CASCADE
);

-- 3. Tabela: EspecialistaPJ (Especialização de Especialista)
CREATE TABLE "EspecialistaPJ" (
    "IDEspecialistaPJ" BIGINT PRIMARY KEY,
    "CNPJ" VARCHAR(18) NOT NULL UNIQUE,
    "RazaoSocial" VARCHAR(150) NOT NULL,
    "NomeFantasia" VARCHAR(150),
    "InscricaoEstadual" VARCHAR(30),
    CONSTRAINT "fk_especialistapj_especialista" FOREIGN KEY ("IDEspecialistaPJ") REFERENCES "Especialista" ("IDEspecialista") ON DELETE CASCADE
);

-- 4. Tabela: JornadaTrabalho
CREATE TABLE "JornadaTrabalho" (
    "IDJornada" BIGSERIAL PRIMARY KEY,
    "IDEspecialista" BIGINT NOT NULL,
    "DiaSemana" INT NOT NULL,
    "HoraInicio" TIME NOT NULL,
    "HoraTermino" TIME NOT NULL,
    CONSTRAINT "fk_jornada_especialista" FOREIGN KEY ("IDEspecialista") REFERENCES "Especialista" ("IDEspecialista") ON DELETE CASCADE
);

-- 5. Tabela Associativa: Especialidade_Especialista (N:N)
CREATE TABLE "Especialidade_Especialista" (
    "IDEspecialista" BIGINT NOT NULL,
    "IDEspecialidade" BIGINT NOT NULL,
    PRIMARY KEY ("IDEspecialista", "IDEspecialidade"),
    CONSTRAINT "fk_especialidade_especialista_especialista" FOREIGN KEY ("IDEspecialista") REFERENCES "Especialista" ("IDEspecialista") ON DELETE CASCADE,
    CONSTRAINT "fk_especialidade_especialista_especialidade" FOREIGN KEY ("IDEspecialidade") REFERENCES "Especialidade" ("IDEspecialidade") ON DELETE CASCADE
);