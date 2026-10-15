-- Categoria e descrição das permissões (agrupamento na tela de perfis) e a permissão nova
-- VISUALIZAR_PACIENTES_VINCULADOS, usada pelo perfil "Assistente Social".

ALTER TABLE "Permissao" ADD COLUMN "Categoria" VARCHAR(50);

-- 1. Permissão nova
INSERT INTO "Permissao" ("Nome", "Descricao")
VALUES ('VISUALIZAR_PACIENTES_VINCULADOS', 'Visualizar só os pacientes vinculados ao próprio usuário')
ON CONFLICT ("Nome") DO NOTHING;

-- 2. Categoria e descrição de todas as permissões do sistema
CREATE TEMPORARY TABLE "PermissaoCategoria" (
    "Nome" VARCHAR(50),
    "Categoria" VARCHAR(50),
    "Descricao" VARCHAR(255)
) ON COMMIT DROP;

INSERT INTO "PermissaoCategoria" ("Nome", "Categoria", "Descricao")
VALUES
    ('ACESSO_TOTAL', 'Segurança', 'Acesso total: libera todas as funcionalidades do sistema'),
    ('GERENCIAR_USUARIOS', 'Segurança', 'Conceder acesso, trocar perfil, gerar senha temporária e retirar acesso de usuários'),
    ('GERENCIAR_PERFIS', 'Segurança', 'Criar, editar e excluir perfis de acesso e suas permissões'),
    ('VISUALIZAR_AUDITORIA', 'Segurança', 'Consultar a trilha de auditoria'),
    ('VISUALIZAR_PACIENTES', 'Pacientes', 'Buscar e ver todos os pacientes e responsáveis'),
    ('VISUALIZAR_PACIENTES_VINCULADOS', 'Pacientes', 'Buscar e ver só os pacientes vinculados ao próprio usuário'),
    ('CADASTRAR_PACIENTE', 'Pacientes', 'Cadastrar pacientes'),
    ('EDITAR_PACIENTE', 'Pacientes', 'Editar pacientes e responsáveis'),
    ('INATIVAR_PACIENTE', 'Pacientes', 'Inativar pacientes e responsáveis'),
    ('VISUALIZAR_HISTORICO_PACIENTE', 'Pacientes', 'Ver o histórico de atividades do paciente'),
    ('VISUALIZAR_TERAPIAS', 'Terapias', 'Listar e ver todas as terapias'),
    ('AGENDAR_TERAPIA', 'Terapias', 'Agendar terapias e verificar conflitos de horário'),
    ('EDITAR_TERAPIA', 'Terapias', 'Remarcar terapias e trocar participantes'),
    ('ALTERAR_STATUS_TERAPIA', 'Terapias', 'Iniciar, concluir e cancelar terapias'),
    ('REGISTRAR_PRESENCA', 'Terapias', 'Registrar presença e falta dos pacientes'),
    ('CADASTRAR_PROFISSIONAL', 'Profissionais', 'Cadastrar profissionais e assistentes sociais'),
    ('CADASTRAR_ESPECIALISTA', 'Profissionais', 'Cadastrar especialistas'),
    ('CADASTRAR_ESPECIALISTA_PJ', 'Profissionais', 'Cadastrar especialistas pessoa jurídica'),
    ('VISUALIZAR_PROFISSIONAIS', 'Profissionais', 'Listar e ver profissionais, especialistas e assistentes sociais'),
    ('EDITAR_PROFISSIONAL', 'Profissionais', 'Editar profissionais, especialistas e assistentes sociais'),
    ('INATIVAR_PROFISSIONAL', 'Profissionais', 'Inativar profissionais, especialistas e assistentes sociais'),
    ('EMITIR_ATESTADO', 'Documentos', 'Emitir atestados'),
    ('EMITIR_RECEITA', 'Documentos', 'Emitir receitas'),
    ('EMITIR_RECUSA', 'Documentos', 'Emitir termos de recusa'),
    ('GERENCIAR_CARGOS', 'Tabelas de apoio', 'Gerenciar cargos e CBOs'),
    ('GERENCIAR_ESCOLAS', 'Tabelas de apoio', 'Gerenciar escolas'),
    ('GERENCIAR_ESPECIALIDADES', 'Tabelas de apoio', 'Gerenciar especialidades');

UPDATE "Permissao" permissao
SET "Categoria" = categoria."Categoria",
    "Descricao" = categoria."Descricao"
FROM "PermissaoCategoria" categoria
WHERE permissao."Nome" = categoria."Nome";

-- 3. Perfil "Assistente Social": passa a ver só os pacientes vinculados (antes, todos).
-- Cria o perfil se ele tiver sido excluído; demais permissões do perfil são mantidas.
INSERT INTO "Perfil" ("Nome")
SELECT 'Assistente Social'
WHERE NOT EXISTS (SELECT 1 FROM "Perfil" perfil WHERE lower(perfil."Nome") = lower('Assistente Social'));

DELETE FROM "Perfil_Permissao"
WHERE "IDPerfil" IN (SELECT "IDPerfil" FROM "Perfil" WHERE lower("Nome") = lower('Assistente Social'))
  AND "IDPermissao" IN (SELECT "IDPermissao" FROM "Permissao" WHERE "Nome" = 'VISUALIZAR_PACIENTES');

INSERT INTO "Perfil_Permissao" ("IDPerfil", "IDPermissao")
SELECT perfil."IDPerfil", permissao."IDPermissao"
FROM "Perfil" perfil
CROSS JOIN "Permissao" permissao
WHERE lower(perfil."Nome") = lower('Assistente Social')
  AND permissao."Nome" IN ('VISUALIZAR_PACIENTES_VINCULADOS', 'VISUALIZAR_HISTORICO_PACIENTE')
ON CONFLICT ("IDPerfil", "IDPermissao") DO NOTHING;
