ALTER TABLE public.tipo_quarto
    ADD CONSTRAINT ck_tipo_quarto_capacidade
        CHECK (capacidade > 0),
    ADD CONSTRAINT ck_tipo_quarto_preco
        CHECK (precodiario > 0);

ALTER TABLE public.quartos
    ADD CONSTRAINT ck_quartos_numero
        CHECK (numero > 0),
    ADD CONSTRAINT ck_quartos_status
        CHECK (status IN ('DISPONIVEL', 'MANUTENCAO', 'INATIVO'));