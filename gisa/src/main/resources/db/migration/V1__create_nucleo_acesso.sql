-- 1. Tabela Base: Pessoa
CREATE TABLE "Pessoa" (
    "IDCadastro" BIGSERIAL PRIMARY KEY,
    "CPF" VARCHAR(14) NOT NULL UNIQUE,
    "Nome" VARCHAR(150) NOT NULL,
    "DataNascimento" DATE NOT NULL,
    "Sexo" CHAR(1) NOT NULL,
    "Celular" VARCHAR(20) NOT NULL,
    "EstadoCivil" VARCHAR(30) NOT NULL,
    "NumCNS" VARCHAR(20),
    "StatusCadastro" VARCHAR(30) NOT NULL
);

-- 2. Tabela: Endereco
CREATE TABLE "Endereco" (
    "IDEndereco" BIGSERIAL PRIMARY KEY,
    "CEP" VARCHAR(10) NOT NULL,
    "Rua" VARCHAR(150) NOT NULL,
    "Cidade" VARCHAR(100) NOT NULL,
    "Bairro" VARCHAR(100) NOT NULL,
    "Estado" VARCHAR(50) NOT NULL,
    "Numero" VARCHAR(20) NOT NULL,
    "Complemento" VARCHAR(100)
);

-- 3. Tabela Associativa: Endereco_Pessoa (N:N)
CREATE TABLE "Endereco_Pessoa" (
    "IDEndereco" BIGINT NOT NULL,
    "IDCadastro" BIGINT NOT NULL,
    PRIMARY KEY ("IDEndereco", "IDCadastro"),
    CONSTRAINT "fk_endereco_pessoa_endereco" FOREIGN KEY ("IDEndereco") REFERENCES "Endereco" ("IDEndereco") ON DELETE CASCADE,
    CONSTRAINT "fk_endereco_pessoa_pessoa" FOREIGN KEY ("IDCadastro") REFERENCES "Pessoa" ("IDCadastro") ON DELETE CASCADE
);

-- 4. Tabela: Perfil
CREATE TABLE "Perfil" (
    "IDPerfil" BIGSERIAL PRIMARY KEY,
    "Nome" VARCHAR(50) NOT NULL UNIQUE
);

-- 5. Tabela: Permissao
CREATE TABLE "Permissao" (
    "IDPermissao" BIGSERIAL PRIMARY KEY,
    "Nome" VARCHAR(50) NOT NULL UNIQUE,
    "Descricao" VARCHAR(255)
);

-- 6. Tabela Associativa: Perfil_Permissao (N:N)
CREATE TABLE "Perfil_Permissao" (
    "IDPerfil" BIGINT NOT NULL,
    "IDPermissao" BIGINT NOT NULL,
    PRIMARY KEY ("IDPerfil", "IDPermissao"),
    CONSTRAINT "fk_perfil_permissao_perfil" FOREIGN KEY ("IDPerfil") REFERENCES "Perfil" ("IDPerfil") ON DELETE CASCADE,
    CONSTRAINT "fk_perfil_permissao_permissao" FOREIGN KEY ("IDPermissao") REFERENCES "Permissao" ("IDPermissao") ON DELETE CASCADE
);

-- 7. Tabela: Usuario
CREATE TABLE "Usuario" (
    "IDCadastro" BIGINT PRIMARY KEY,
    "Senha" VARCHAR(255) NOT NULL,
    "IDPerfil" BIGINT NOT NULL,
    CONSTRAINT "fk_usuario_pessoa" FOREIGN KEY ("IDCadastro") REFERENCES "Pessoa" ("IDCadastro") ON DELETE CASCADE,
    CONSTRAINT "fk_usuario_perfil" FOREIGN KEY ("IDPerfil") REFERENCES "Perfil" ("IDPerfil") ON DELETE RESTRICT
);