ALTER TABLE "Cargo" ADD CONSTRAINT "uk_cargo_nome" UNIQUE ("Nome");

INSERT INTO "CBO" ("CodigoCBO", "TituloCBO")
VALUES ('999999', 'Cargo de Teste - Desenvolvimento')
ON CONFLICT ("CodigoCBO") DO NOTHING;

INSERT INTO "Cargo" ("Nome", "CodigoCBO")
VALUES ('Administrador de Sistema', '999999')
ON CONFLICT ("Nome") DO NOTHING;
