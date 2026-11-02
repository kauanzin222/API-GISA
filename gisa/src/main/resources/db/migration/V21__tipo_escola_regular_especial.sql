-- O tipo de escola passa a ser REGULAR ou ESPECIAL (relatório da visita 2 à APAE). Pública/particular
-- não diz se a escola é especial: as escolas existentes viram REGULAR e podem ser corrigidas na edição
UPDATE "Escola" SET "TipoEscola" = 'REGULAR';
