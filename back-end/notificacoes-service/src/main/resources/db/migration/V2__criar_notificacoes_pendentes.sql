CREATE TABLE public.notificacao (
    evento_id uuid PRIMARY KEY,
    cliente_id integer NOT NULL,
    criado_em timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enviado_em timestamptz,
    tentativas integer NOT NULL DEFAULT 0,
    proxima_tentativa_em timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notificacao_evento
        FOREIGN KEY (evento_id)
        REFERENCES public.evento_recebido (evento_id),

    CONSTRAINT ck_notificacao_cliente
        CHECK (cliente_id > 0),

    CONSTRAINT ck_notificacao_tentativas
        CHECK (tentativas >= 0)
);

CREATE INDEX ix_notificacao_pendente
    ON public.notificacao (proxima_tentativa_em, criado_em)
    WHERE enviado_em IS NULL;

-- Prepara notificações para eventos recebidos antes desta migração.
INSERT INTO public.notificacao (evento_id, cliente_id)
SELECT evento_id, CAST(payload ->> 'idCliente' AS integer)
FROM public.evento_recebido;