-- 1. Tabela: AssistenteSocial (Especialização de Profissional, como Especialista).
-- Um assistente social é só assistente social: não acumula especialista nem especialista PJ.
CREATE TABLE "AssistenteSocial" (
    "IDAssistenteSocial" BIGINT PRIMARY KEY,
    -- Número do CRESS
    "RegistroConselho" VARCHAR(50) NOT NULL,
    CONSTRAINT "fk_assistente_social_profissional" FOREIGN KEY ("IDAssistenteSocial") REFERENCES "Profissional" ("IDProfissional") ON DELETE CASCADE
);

-- 2. Tabela Associativa: Paciente_AssistenteSocial (N:N com histórico).
-- Desvincular não apaga a linha: preenche "DataFim". Revincular cria uma linha nova.
CREATE TABLE "Paciente_AssistenteSocial" (
    "IDVinculo" BIGSERIAL PRIMARY KEY,
    "IDPaciente" BIGINT NOT NULL,
    "IDAssistenteSocial" BIGINT NOT NULL,
    "DataInicio" DATE NOT NULL,
    -- Nulo = vínculo ativo
    "DataFim" DATE,
    CONSTRAINT "fk_paciente_assistente_social_paciente" FOREIGN KEY ("IDPaciente") REFERENCES "Paciente" ("IDPaciente") ON DELETE CASCADE,
    CONSTRAINT "fk_paciente_assistente_social_assistente" FOREIGN KEY ("IDAssistenteSocial") REFERENCES "AssistenteSocial" ("IDAssistenteSocial") ON DELETE CASCADE
);

-- No máximo um vínculo ativo por par paciente/assistente social
CREATE UNIQUE INDEX "uk_paciente_assistente_social_ativo"
    ON "Paciente_AssistenteSocial" ("IDPaciente", "IDAssistenteSocial")
    WHERE "DataFim" IS NULL;

CREATE INDEX "idx_paciente_assistente_social_assistente"
    ON "Paciente_AssistenteSocial" ("IDAssistenteSocial");
