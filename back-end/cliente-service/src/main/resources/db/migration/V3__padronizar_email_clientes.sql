LOCK TABLE public.clientes IN ACCESS EXCLUSIVE MODE;

DO $$
BEGIN
    IF EXISTS (
        SELECT lower(email)
        FROM public.clientes
        GROUP BY lower(email)
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Existem clientes com e-mails iguais ao ignorar maiúsculas e minúsculas';
    END IF;
END $$;

UPDATE public.clientes
SET email = lower(email),
    versao = versao + 1
WHERE email <> lower(email);

ALTER TABLE public.clientes
    ADD CONSTRAINT ck_clientes_email_minusculo
    CHECK (email = lower(email));