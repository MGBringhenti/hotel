ALTER TABLE public.quartos
    DROP CONSTRAINT ck_quartos_status;

UPDATE public.quartos
SET status = 'INATIVO',
    versao = versao + 1
WHERE status = 'MANUTENCAO';

ALTER TABLE public.quartos
    ADD CONSTRAINT ck_quartos_status
    CHECK (status IN ('DISPONIVEL', 'INATIVO'));