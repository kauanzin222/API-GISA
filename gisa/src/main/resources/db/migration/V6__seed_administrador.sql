-- Credencial inicial de desenvolvimento:
-- CPF: 99999999999
-- Senha: Admin@12345
-- A senha abaixo esta armazenada somente como hash BCrypt.

INSERT INTO "Perfil" ("Nome")
VALUES ('Administrador')
ON CONFLICT ("Nome") DO NOTHING;

INSERT INTO "Permissao" ("Nome", "Descricao")
VALUES
    ('ACESSO_TOTAL', 'Acesso total aos recursos da API'),
    ('CADASTRAR_PROFISSIONAL', 'Cadastrar profissionais'),
    ('CADASTRAR_ESPECIALISTA', 'Cadastrar especialistas'),
    ('CADASTRAR_ESPECIALISTA_PJ', 'Cadastrar especialistas pessoa juridica'),
    ('CADASTRAR_USUARIO', 'Cadastrar usuarios')
ON CONFLICT ("Nome") DO NOTHING;

INSERT INTO "Perfil_Permissao" ("IDPerfil", "IDPermissao")
SELECT perfil."IDPerfil", permissao."IDPermissao"
FROM "Perfil" perfil
CROSS JOIN "Permissao" permissao
WHERE perfil."Nome" = 'Administrador'
  AND permissao."Nome" IN (
      'ACESSO_TOTAL',
      'CADASTRAR_PROFISSIONAL',
      'CADASTRAR_ESPECIALISTA',
      'CADASTRAR_ESPECIALISTA_PJ',
      'CADASTRAR_USUARIO'
  )
ON CONFLICT ("IDPerfil", "IDPermissao") DO NOTHING;

INSERT INTO "Pessoa" (
    "CPF",
    "Nome",
    "DataNascimento",
    "Sexo",
    "Celular",
    "EstadoCivil",
    "StatusCadastro"
)
VALUES (
    '99999999999',
    'Administrador do Sistema',
    DATE '2000-01-01',
    'O',
    '00000000000',
    'SOLTEIRO',
    'ATIVO'
)
ON CONFLICT ("CPF") DO NOTHING;

INSERT INTO "Usuario" ("IDCadastro", "Senha", "IDPerfil")
SELECT pessoa."IDCadastro",
       '$2a$10$4wCswWQZwGlwvJIbbYfgmuY4r4qmMPXDzvgl2i9I3IVyWY1f3JMCa',
       perfil."IDPerfil"
FROM "Pessoa" pessoa
CROSS JOIN "Perfil" perfil
WHERE pessoa."CPF" = '99999999999'
  AND perfil."Nome" = 'Administrador'
ON CONFLICT ("IDCadastro") DO NOTHING;
