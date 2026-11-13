CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE public.reserva (
    id serial PRIMARY KEY,
    id_cliente integer NOT NULL,
    id_quarto integer NOT NULL,
    data_inicio date NOT NULL,
    data_final date NOT NULL,
    valor_total numeric(14, 2) NOT NULL,
    status varchar(50) NOT NULL,
    preco_diario_aplicado numeric(12, 2) NOT NULL,
    versao bigint NOT NULL,

    CONSTRAINT reserva_status_check
        CHECK (
            status IN (
                'PENDENTE',
                'CONFIRMADA',
                'CANCELADA',
                'CONCLUIDA'
            )
        ),

    CONSTRAINT ck_reserva_periodo
        CHECK (data_final > data_inicio),

    CONSTRAINT ck_reserva_diaria
        CHECK (preco_diario_aplicado > 0),

    CONSTRAINT ck_reserva_total
        CHECK (valor_total > 0),

    CONSTRAINT ck_reserva_calculo_total
        CHECK (
            valor_total =
            preco_diario_aplicado * (data_final - data_inicio)
        ),

    CONSTRAINT ck_reserva_versao
        CHECK (versao >= 0),

    CONSTRAINT ck_reserva_ids
        CHECK (id_cliente > 0 AND id_quarto > 0),

    CONSTRAINT ex_reserva_quarto_periodo
        EXCLUDE USING gist (
            id_quarto WITH =,
            daterange(data_inicio, data_final, '[)') WITH &&
        )
        WHERE (
            status IN ('PENDENTE', 'CONFIRMADA', 'CONCLUIDA')
        )
);

CREATE INDEX ix_reserva_cliente
    ON public.reserva (id_cliente);