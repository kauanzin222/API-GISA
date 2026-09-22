-- 1. Tabela: Escola
CREATE TABLE "Escola" (
    "IDEscola" BIGSERIAL PRIMARY KEY,
    "Nome" VARCHAR(150) NOT NULL,
    "TipoEscola" VARCHAR(50) NOT NULL,
    "Telefone" VARCHAR(20)
);

-- 2. Tabela: CID
CREATE TABLE "CID" (
    "CodigoCID" VARCHAR(20) PRIMARY KEY,
    "Descricao" VARCHAR(255) NOT NULL
);

-- 3. Tabela: Responsavel (Especialização de Pessoa)
CREATE TABLE "Responsavel" (
    "IDResponsavel" BIGINT PRIMARY KEY,
    "Ocupacao" VARCHAR(100),
    CONSTRAINT "fk_responsavel_pessoa" FOREIGN KEY ("IDResponsavel") REFERENCES "Pessoa" ("IDCadastro") ON DELETE CASCADE
);

-- 4. Tabela: Paciente (Especialização de Pessoa)
CREATE TABLE "Paciente" (
    "IDPaciente" BIGINT PRIMARY KEY,
    "IDEscola" BIGINT,
    "StatusPaciente" VARCHAR(30) NOT NULL,
    "DataCadastro" DATE NOT NULL,
    "Convenio" BOOLEAN,
    CONSTRAINT "fk_paciente_pessoa" FOREIGN KEY ("IDPaciente") REFERENCES "Pessoa" ("IDCadastro") ON DELETE CASCADE,
    CONSTRAINT "fk_paciente_escola" FOREIGN KEY ("IDEscola") REFERENCES "Escola" ("IDEscola") ON DELETE SET NULL
);

-- 5. Tabela: Prontuario (1:1 com Paciente via IDPaciente)
CREATE TABLE "Prontuario" (
    "IDPaciente" BIGINT PRIMARY KEY,
    "Alergias" TEXT,
    "Comorbidade" TEXT,
    "Mobilidade" VARCHAR(100),
    CONSTRAINT "fk_prontuario_paciente" FOREIGN KEY ("IDPaciente") REFERENCES "Paciente" ("IDPaciente") ON DELETE CASCADE
);

-- 6. Tabela Associativa: Vinculo_Responsavel (N:N entre Responsavel e Paciente)
CREATE TABLE "Vinculo_Responsavel" (
    "IDResponsavel" BIGINT NOT NULL,
    "IDPaciente" BIGINT NOT NULL,
    "GrauParentesco" VARCHAR(50) NOT NULL,
    PRIMARY KEY ("IDResponsavel", "IDPaciente"),
    CONSTRAINT "fk_vinculo_responsavel" FOREIGN KEY ("IDResponsavel") REFERENCES "Responsavel" ("IDResponsavel") ON DELETE CASCADE,
    CONSTRAINT "fk_vinculo_paciente" FOREIGN KEY ("IDPaciente") REFERENCES "Paciente" ("IDPaciente") ON DELETE CASCADE
);

-- 7. Tabela Associativa: Paciente_CID (N:N entre Paciente e CID)
CREATE TABLE "Paciente_CID" (
    "CodigoCID" VARCHAR(20) NOT NULL,
    "IDPaciente" BIGINT NOT NULL,
    PRIMARY KEY ("CodigoCID", "IDPaciente"),
    CONSTRAINT "fk_pacientecid_cid" FOREIGN KEY ("CodigoCID") REFERENCES "CID" ("CodigoCID") ON DELETE CASCADE,
    CONSTRAINT "fk_pacientecid_paciente" FOREIGN KEY ("IDPaciente") REFERENCES "Paciente" ("IDPaciente") ON DELETE CASCADE
);