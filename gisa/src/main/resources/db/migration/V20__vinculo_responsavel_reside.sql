-- Se o responsável mora com o paciente. Enquanto for TRUE, a API mantém os endereços do responsável
-- iguais aos do paciente: quando o endereço do paciente muda, os responsáveis que residem com ele vão junto.
ALTER TABLE "Vinculo_Responsavel" ADD COLUMN "ResideComPaciente" BOOLEAN NOT NULL DEFAULT FALSE;

-- Vínculos existentes: reside quem compartilha algum endereço com o paciente, ou quando nenhum dos dois tem endereço
UPDATE "Vinculo_Responsavel" AS vinculo
SET "ResideComPaciente" = TRUE
WHERE EXISTS (
        SELECT 1
        FROM "Endereco_Pessoa" AS do_paciente
        JOIN "Endereco_Pessoa" AS do_responsavel ON do_responsavel."IDEndereco" = do_paciente."IDEndereco"
        WHERE do_paciente."IDCadastro" = vinculo."IDPaciente"
          AND do_responsavel."IDCadastro" = vinculo."IDResponsavel")
   OR (NOT EXISTS (SELECT 1 FROM "Endereco_Pessoa" WHERE "IDCadastro" = vinculo."IDPaciente")
       AND NOT EXISTS (SELECT 1 FROM "Endereco_Pessoa" WHERE "IDCadastro" = vinculo."IDResponsavel"));

-- Endereço só existe enquanto alguém mora nele: remove os que ficaram sem moradores
DELETE FROM "Endereco" AS endereco
WHERE NOT EXISTS (SELECT 1 FROM "Endereco_Pessoa" WHERE "IDEndereco" = endereco."IDEndereco");
