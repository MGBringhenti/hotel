CREATE TABLE public.evento_recebido (
    evento_id uuid PRIMARY KEY,
    tipo varchar(50) NOT NULL,
    reserva_id integer NOT NULL,
    reserva_versao bigint NOT NULL,
    payload jsonb NOT NULL,
    ocorrido_em timestamptz NOT NULL,
    recebido_em timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_evento_recebido_reserva_id
        CHECK (reserva_id > 0),

    CONSTRAINT ck_evento_recebido_reserva_versao
        CHECK (reserva_versao >= 0),

    CONSTRAINT ck_evento_recebido_payload
        CHECK (jsonb_typeof(payload) = 'object')
);

CREATE INDEX ix_evento_recebido_reserva
    ON public.evento_recebido (reserva_id, reserva_versao);