ALTER TABLE public.clientes
    ADD CONSTRAINT ck_clientes_cpf_formato
    CHECK (cpf ~ '^[0-9]{11}$');