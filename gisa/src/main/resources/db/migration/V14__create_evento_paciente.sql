-- Linha do tempo do paciente (GET /api/pacientes/{id}/historico). Não é a auditoria: são eventos
-- de negócio com descrição legível, gravados na mesma transação da operação que os gerou.
-- Nunca guarda dado de saúde (CIDs, prontuário, alergias) nem textos livres digitados pelos
-- usuários (justificativas de falta, motivos de cancelamento), que podem conter esse tipo de dado.
CREATE TABLE "EventoPaciente" (
    "IDEvento" BIGSERIAL PRIMARY KEY,
    "IDPaciente" BIGINT NOT NULL,
    "DataHora" TIMESTAMP WITH TIME ZONE NOT NULL,
    "Tipo" VARCHAR(40) NOT NULL,
    "Descricao" VARCHAR(500) NOT NULL,
    -- Só nos eventos de terapia
    "IDTerapia" BIGINT,
    -- Quem fez a ação; nulo se a pessoa for excluída ou se não houver usuário identificado
    "IDUsuario" BIGINT,
    CONSTRAINT "fk_evento_paciente_paciente" FOREIGN KEY ("IDPaciente") REFERENCES "Paciente" ("IDPaciente") ON DELETE CASCADE,
    CONSTRAINT "fk_evento_paciente_terapia" FOREIGN KEY ("IDTerapia") REFERENCES "Terapia" ("IDTerapia") ON DELETE SET NULL,
    CONSTRAINT "fk_evento_paciente_usuario" FOREIGN KEY ("IDUsuario") REFERENCES "Pessoa" ("IDCadastro") ON DELETE SET NULL
);

CREATE INDEX "idx_evento_paciente_data_hora" ON "EventoPaciente" ("IDPaciente", "DataHora" DESC);
CREATE INDEX "idx_evento_paciente_terapia" ON "EventoPaciente" ("IDTerapia");
CREATE INDEX "idx_evento_paciente_usuario" ON "EventoPaciente" ("IDUsuario");
