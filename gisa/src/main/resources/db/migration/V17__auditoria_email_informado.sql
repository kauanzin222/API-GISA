-- E-mail digitado nas tentativas de login (o login passou a ser por e-mail). Nulo nos demais
-- registros e nos registros anteriores a esta versão.
ALTER TABLE "LogAuditoria" ADD COLUMN "EmailInformado" VARCHAR(255);
