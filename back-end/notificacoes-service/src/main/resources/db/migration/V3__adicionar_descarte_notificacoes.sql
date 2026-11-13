ALTER TABLE public.notificacao
    ADD COLUMN descartado_em timestamptz,
    ADD COLUMN motivo_descarte varchar(100);

ALTER TABLE public.notificacao
    ADD CONSTRAINT ck_notificacao_descarte
    CHECK (
        (descartado_em IS NULL AND motivo_descarte IS NULL)
        OR
        (descartado_em IS NOT NULL AND motivo_descarte IS NOT NULL)
    );

ALTER TABLE public.notificacao
    ADD CONSTRAINT ck_notificacao_resultado
    CHECK (
        enviado_em IS NULL OR descartado_em IS NULL
    );

DROP INDEX public.ix_notificacao_pendente;

CREATE INDEX ix_notificacao_pendente
    ON public.notificacao (proxima_tentativa_em, criado_em)
    WHERE enviado_em IS NULL
      AND descartado_em IS NULL;