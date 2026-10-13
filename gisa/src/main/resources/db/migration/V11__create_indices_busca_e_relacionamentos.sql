-- Índices para as buscas e para os relacionamentos usados nas listagens.

-- 1. Busca por trecho (LIKE '%texto%')
-- Índices B-tree comuns não ajudam quando o padrão começa com '%'. A extensão pg_trgm
-- permite índices GIN por trigramas, usados nessas buscas. As expressões abaixo são as
-- mesmas geradas pelas consultas: lower("Nome") nas buscas de pessoas e CBO, e upper("Nome")
-- na busca de escolas (gerada pelo Spring Data com IgnoreCase).
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS "idx_pessoa_nome_trgm"
    ON "Pessoa" USING gin (lower("Nome") gin_trgm_ops);

CREATE INDEX IF NOT EXISTS "idx_pessoa_cpf_trgm"
    ON "Pessoa" USING gin ("CPF" gin_trgm_ops);

CREATE INDEX IF NOT EXISTS "idx_escola_nome_trgm"
    ON "Escola" USING gin (upper("Nome") gin_trgm_ops);

CREATE INDEX IF NOT EXISTS "idx_cbo_titulo_trgm"
    ON "CBO" USING gin (lower("TituloCBO") gin_trgm_ops);

-- Busca de CBO pelo início do código (LIKE 'texto%')
CREATE INDEX IF NOT EXISTS "idx_cbo_codigo_prefixo"
    ON "CBO" ("CodigoCBO" text_pattern_ops);

-- 2. Terapias: ordenação por data e filtro por status
CREATE INDEX IF NOT EXISTS "idx_terapia_data_inicio"
    ON "Terapia" ("DataHoraInicio");

CREATE INDEX IF NOT EXISTS "idx_terapia_status_data_inicio"
    ON "Terapia" ("Status", "DataHoraInicio");

-- 3. Chaves estrangeiras
-- O PostgreSQL não indexa FKs automaticamente. Nas tabelas associativas, a chave primária
-- composta só serve para buscas pela primeira coluna; os índices abaixo cobrem a segunda.
CREATE INDEX IF NOT EXISTS "idx_endereco_pessoa_cadastro"
    ON "Endereco_Pessoa" ("IDCadastro");

CREATE INDEX IF NOT EXISTS "idx_vinculo_responsavel_paciente"
    ON "Vinculo_Responsavel" ("IDPaciente");

CREATE INDEX IF NOT EXISTS "idx_paciente_cid_paciente"
    ON "Paciente_CID" ("IDPaciente");

CREATE INDEX IF NOT EXISTS "idx_paciente_terapia_terapia"
    ON "Paciente_Terapia" ("IDTerapia");

CREATE INDEX IF NOT EXISTS "idx_especialista_terapia_especialista"
    ON "Especialista_Terapia" ("IDEspecialista");

CREATE INDEX IF NOT EXISTS "idx_especialidade_especialista_especialidade"
    ON "Especialidade_Especialista" ("IDEspecialidade");

CREATE INDEX IF NOT EXISTS "idx_perfil_permissao_permissao"
    ON "Perfil_Permissao" ("IDPermissao");

CREATE INDEX IF NOT EXISTS "idx_paciente_escola"
    ON "Paciente" ("IDEscola");

CREATE INDEX IF NOT EXISTS "idx_profissional_cargo"
    ON "Profissional" ("IDCargo");

CREATE INDEX IF NOT EXISTS "idx_cargo_cbo"
    ON "Cargo" ("CodigoCBO");

CREATE INDEX IF NOT EXISTS "idx_usuario_perfil"
    ON "Usuario" ("IDPerfil");

CREATE INDEX IF NOT EXISTS "idx_jornada_especialista"
    ON "JornadaTrabalho" ("IDEspecialista");
