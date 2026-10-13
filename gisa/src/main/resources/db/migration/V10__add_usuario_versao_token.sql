-- Versão das credenciais do usuário. Cada token JWT carrega a versão vigente quando
-- foi emitido; trocar a senha incrementa a versão e invalida os tokens anteriores.
ALTER TABLE "Usuario"
ADD COLUMN "VersaoToken" INTEGER NOT NULL DEFAULT 0;
