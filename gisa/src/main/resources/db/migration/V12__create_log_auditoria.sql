-- Trilha de auditoria: quem fez o quê, quando e com qual resultado.
-- O corpo das requisições não é gravado (pode conter senhas e dados de saúde).
CREATE TABLE "LogAuditoria" (
    "IDLog" BIGSERIAL PRIMARY KEY,
    "DataHora" TIMESTAMP WITH TIME ZONE NOT NULL,
    -- Nulo quando não há usuário identificado (ex.: login com CPF ausente)
    "CPFUsuario" VARCHAR(14),
    "Acao" VARCHAR(20) NOT NULL,
    "Metodo" VARCHAR(10) NOT NULL,
    "Caminho" VARCHAR(255) NOT NULL,
    "StatusHttp" INT NOT NULL,
    "IP" VARCHAR(45)
);

CREATE INDEX "idx_log_auditoria_data_hora" ON "LogAuditoria" ("DataHora" DESC);
CREATE INDEX "idx_log_auditoria_cpf" ON "LogAuditoria" ("CPFUsuario", "DataHora" DESC);
