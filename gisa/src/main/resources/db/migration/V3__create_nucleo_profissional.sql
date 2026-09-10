-- 1. Tabela: CBO
CREATE TABLE "CBO" (
    "CodigoCBO" VARCHAR(20) PRIMARY KEY,
    "TituloCBO" VARCHAR(255) NOT NULL
);

-- 2. Tabela: Cargo
CREATE TABLE "Cargo" (
    "IDCargo" BIGSERIAL PRIMARY KEY,
    "Nome" VARCHAR(150) NOT NULL,
    "CodigoCBO" VARCHAR(20) NOT NULL,
    CONSTRAINT "fk_cargo_cbo" FOREIGN KEY ("CodigoCBO") REFERENCES "CBO" ("CodigoCBO") ON DELETE RESTRICT
);

-- 3. Tabela: Profissional (Especialização de Pessoa)
CREATE TABLE "Profissional" (
    "IDProfissional" BIGINT PRIMARY KEY,
    "IDCargo" BIGINT NOT NULL,
    CONSTRAINT "fk_profissional_pessoa" FOREIGN KEY ("IDProfissional") REFERENCES "Pessoa" ("IDCadastro") ON DELETE CASCADE,
    CONSTRAINT "fk_profissional_cargo" FOREIGN KEY ("IDCargo") REFERENCES "Cargo" ("IDCargo") ON DELETE RESTRICT
);