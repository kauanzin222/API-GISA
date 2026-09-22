ALTER TABLE "Especialidade" ADD CONSTRAINT "uk_especialidade_nome" UNIQUE ("Nome");

INSERT INTO "Especialidade" ("Nome", "Descricao")
VALUES
    ('Psicologia', 'Atendimento psicológico e acompanhamento terapêutico'),
    ('Fonoaudiologia', 'Avaliação e terapia da comunicação, fala e linguagem'),
    ('Terapia Ocupacional', 'Desenvolvimento de habilidades motoras e de autonomia'),
    ('Fisioterapia', 'Reabilitação motora e funcional'),
    ('Psicopedagogia', 'Acompanhamento de dificuldades de aprendizagem')
ON CONFLICT ("Nome") DO NOTHING;