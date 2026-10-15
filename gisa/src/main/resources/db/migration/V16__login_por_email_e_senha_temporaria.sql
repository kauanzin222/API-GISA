-- Login por e-mail, senha temporária e redefinição de senha.

-- 1. O e-mail passa de Profissional para Pessoa: o login é feito pelo e-mail e o administrador
-- inicial (cadastro só de Pessoa) também precisa de um. Pacientes e responsáveis ficam sem e-mail.
ALTER TABLE "Pessoa" ADD COLUMN "Email" VARCHAR(255);

UPDATE "Pessoa" pessoa
SET "Email" = profissional."Email"
FROM "Profissional" profissional
WHERE profissional."IDProfissional" = pessoa."IDCadastro";

ALTER TABLE "Profissional" DROP COLUMN "Email";

ALTER TABLE "Pessoa" ADD CONSTRAINT "uk_pessoa_email" UNIQUE ("Email");

-- 2. E-mail do administrador inicial (CPF 99999999999), se ainda não tiver um
UPDATE "Pessoa"
SET "Email" = 'admin@gisa.local'
WHERE "CPF" = '99999999999'
  AND "Email" IS NULL
  AND NOT EXISTS (SELECT 1 FROM "Pessoa" outra WHERE lower(outra."Email") = 'admin@gisa.local');

-- Qualquer outro usuário de acesso sem e-mail (não deveria existir) recebe um provisório, para não
-- ficar impedido de entrar; o administrador pode trocá-lo depois pelo PATCH do cadastro
UPDATE "Pessoa" pessoa
SET "Email" = 'usuario.' || pessoa."IDCadastro" || '@gisa.local'
WHERE pessoa."Email" IS NULL
  AND EXISTS (SELECT 1 FROM "Usuario" usuario WHERE usuario."IDCadastro" = pessoa."IDCadastro");

-- 3. Senha temporária: gerada pelo back ao conceder acesso; vale 7 dias e precisa ser trocada
-- no primeiro acesso. Usuários existentes continuam com a senha atual (não temporária).
ALTER TABLE "Usuario" ADD COLUMN "SenhaTemporaria" BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE "Usuario" ADD COLUMN "SenhaTemporariaExpiraEm" TIMESTAMP WITH TIME ZONE;

-- 4. Tokens de redefinição de senha (primeiro acesso e "esqueci minha senha").
-- Só o hash (SHA-256) do token é guardado; o token em si vai para o usuário e não fica no banco.
CREATE TABLE "TokenRedefinicaoSenha" (
    "IDToken" BIGSERIAL PRIMARY KEY,
    "IDUsuario" BIGINT NOT NULL,
    "TokenHash" VARCHAR(64) NOT NULL UNIQUE,
    -- PRIMEIRO_ACESSO ou ESQUECI_SENHA
    "Origem" VARCHAR(20) NOT NULL,
    "ExpiraEm" TIMESTAMP WITH TIME ZONE NOT NULL,
    -- Preenchido quando o token é usado ou invalidado por um token mais novo
    "UsadoEm" TIMESTAMP WITH TIME ZONE,
    "CriadoEm" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT "fk_token_redefinicao_usuario" FOREIGN KEY ("IDUsuario") REFERENCES "Usuario" ("IDCadastro") ON DELETE CASCADE
);

CREATE INDEX "idx_token_redefinicao_usuario" ON "TokenRedefinicaoSenha" ("IDUsuario");
