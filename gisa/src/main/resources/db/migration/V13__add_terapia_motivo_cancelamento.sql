-- Motivo informado ao cancelar uma terapia (PATCH /api/terapias/{id}/status).
-- Nulo nas terapias que não foram canceladas.
ALTER TABLE "Terapia" ADD COLUMN "MotivoCancelamento" VARCHAR(255);
