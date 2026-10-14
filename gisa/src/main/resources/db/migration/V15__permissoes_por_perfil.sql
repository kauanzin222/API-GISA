-- Lista definitiva de permissões e perfis padrão. A partir desta versão, toda rota exige
-- permissão (ver docs/00-GERAL.md); ACESSO_TOTAL continua valendo por todas.

-- 1. A permissão 5 passa a cobrir toda a administração de acessos
UPDATE "Permissao"
SET "Nome" = 'GERENCIAR_USUARIOS',
    "Descricao" = 'Gerenciar usuários de acesso: conceder, trocar perfil, redefinir senha e retirar acesso'
WHERE "Nome" = 'CADASTRAR_USUARIO';

-- 2. Permissões novas
INSERT INTO "Permissao" ("Nome", "Descricao")
VALUES
    ('VISUALIZAR_PACIENTES', 'Visualizar pacientes e responsáveis'),
    ('CADASTRAR_PACIENTE', 'Cadastrar pacientes'),
    ('EDITAR_PACIENTE', 'Editar pacientes e responsáveis'),
    ('INATIVAR_PACIENTE', 'Inativar pacientes e responsáveis'),
    ('VISUALIZAR_HISTORICO_PACIENTE', 'Visualizar o histórico de atividades do paciente'),
    ('VISUALIZAR_PROFISSIONAIS', 'Visualizar profissionais e especialistas'),
    ('EDITAR_PROFISSIONAL', 'Editar profissionais e especialistas'),
    ('INATIVAR_PROFISSIONAL', 'Inativar profissionais e especialistas'),
    ('VISUALIZAR_TERAPIAS', 'Visualizar todas as terapias'),
    ('AGENDAR_TERAPIA', 'Agendar terapias'),
    ('EDITAR_TERAPIA', 'Remarcar terapias e trocar participantes'),
    ('ALTERAR_STATUS_TERAPIA', 'Alterar o status de terapias, inclusive cancelar'),
    ('REGISTRAR_PRESENCA', 'Registrar presença e falta dos pacientes nas terapias'),
    ('EMITIR_ATESTADO', 'Emitir atestados'),
    ('EMITIR_RECEITA', 'Emitir receitas'),
    ('EMITIR_RECUSA', 'Emitir termos de recusa'),
    ('GERENCIAR_CARGOS', 'Gerenciar cargos e CBOs'),
    ('GERENCIAR_PERFIS', 'Gerenciar perfis de acesso e suas permissões'),
    ('GERENCIAR_ESCOLAS', 'Gerenciar escolas'),
    ('GERENCIAR_ESPECIALIDADES', 'Gerenciar especialidades'),
    ('VISUALIZAR_AUDITORIA', 'Consultar a trilha de auditoria')
ON CONFLICT ("Nome") DO NOTHING;

-- 3. Perfis padrão. A API compara nomes de perfil sem diferenciar maiúsculas, então a busca
-- também não diferencia: um "recepção" criado pela API é reaproveitado, não duplicado
INSERT INTO "Perfil" ("Nome")
SELECT padrao."Nome"
FROM (VALUES ('Administrador'), ('Recepção'), ('Especialista'), ('Assistente Social')) AS padrao ("Nome")
WHERE NOT EXISTS (
    SELECT 1 FROM "Perfil" perfil WHERE lower(perfil."Nome") = lower(padrao."Nome")
);

-- 4. Substitui as permissões de cada perfil padrão
CREATE TEMPORARY TABLE "PermissaoPadrao" ("Perfil" VARCHAR(50), "Permissao" VARCHAR(50)) ON COMMIT DROP;

INSERT INTO "PermissaoPadrao" ("Perfil", "Permissao")
VALUES
    ('Administrador', 'ACESSO_TOTAL'),
    ('Recepção', 'VISUALIZAR_PACIENTES'),
    ('Recepção', 'EDITAR_PACIENTE'),
    ('Recepção', 'VISUALIZAR_HISTORICO_PACIENTE'),
    ('Recepção', 'VISUALIZAR_TERAPIAS'),
    ('Recepção', 'AGENDAR_TERAPIA'),
    ('Recepção', 'EDITAR_TERAPIA'),
    ('Recepção', 'ALTERAR_STATUS_TERAPIA'),
    ('Recepção', 'REGISTRAR_PRESENCA'),
    ('Recepção', 'EMITIR_RECUSA'),
    ('Especialista', 'VISUALIZAR_PACIENTES'),
    ('Especialista', 'VISUALIZAR_HISTORICO_PACIENTE'),
    ('Especialista', 'EMITIR_ATESTADO'),
    ('Especialista', 'EMITIR_RECEITA'),
    ('Assistente Social', 'VISUALIZAR_PACIENTES'),
    ('Assistente Social', 'VISUALIZAR_HISTORICO_PACIENTE');

DELETE FROM "Perfil_Permissao"
WHERE "IDPerfil" IN (
    SELECT perfil."IDPerfil"
    FROM "Perfil" perfil
    WHERE lower(perfil."Nome") IN (SELECT DISTINCT lower("Perfil") FROM "PermissaoPadrao")
);

INSERT INTO "Perfil_Permissao" ("IDPerfil", "IDPermissao")
SELECT perfil."IDPerfil", permissao."IDPermissao"
FROM "PermissaoPadrao" padrao
JOIN "Perfil" perfil ON lower(perfil."Nome") = lower(padrao."Perfil")
JOIN "Permissao" permissao ON permissao."Nome" = padrao."Permissao";

-- 5. Momento em que o usuário de acesso foi criado. Retirar o acesso apaga o usuário, e um
-- acesso concedido de novo recomeça a versão do token em 0: sem esta data, os tokens antigos
-- (mesmo CPF, versão 0) voltariam a valer. Tokens emitidos antes dela são recusados.
-- Usuários já existentes recebem uma data antiga, para não derrubar as sessões abertas.
ALTER TABLE "Usuario"
ADD COLUMN "DataCriacao" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE "Usuario" ALTER COLUMN "DataCriacao" SET DEFAULT now();
