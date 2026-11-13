ALTER TABLE public.quartos
    ADD COLUMN versao bigint;

UPDATE public.quartos
SET versao = 0;

ALTER TABLE public.quartos
    ALTER COLUMN versao SET NOT NULL,
    ADD CONSTRAINT ck_quartos_versao CHECK (versao >= 0);