ALTER TABLE public.tipo_quarto
    ADD COLUMN versao bigint;

UPDATE public.tipo_quarto
SET versao = 0;

ALTER TABLE public.tipo_quarto
    ALTER COLUMN versao SET NOT NULL,
    ADD CONSTRAINT ck_tipo_quarto_versao CHECK (versao >= 0);