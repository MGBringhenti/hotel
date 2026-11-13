CREATE TABLE public.outbox_evento (
    id uuid PRIMARY KEY,
    reserva_id integer NOT NULL,
    reserva_versao bigint NOT NULL,
    tipo varchar(50) NOT NULL,
    payload jsonb NOT NULL,
    criado_em timestamptz NOT NULL,
    publicado_em timestamptz,
    tentativas integer NOT NULL DEFAULT 0,
    proxima_tentativa_em timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_outbox_reserva
        FOREIGN KEY (reserva_id)
        REFERENCES public.reserva (id),

    CONSTRAINT ck_outbox_reserva_versao
        CHECK (reserva_versao >= 0),

    CONSTRAINT ck_outbox_tentativas
        CHECK (tentativas >= 0),

    CONSTRAINT ck_outbox_payload
        CHECK (jsonb_typeof(payload) = 'object'),

    CONSTRAINT uk_outbox_reserva_versao
        UNIQUE (reserva_id, reserva_versao)
);

CREATE INDEX ix_outbox_pendentes
    ON public.outbox_evento (proxima_tentativa_em, criado_em)
    WHERE publicado_em IS NULL;