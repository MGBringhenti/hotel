ALTER TABLE public.usuarios
    ADD COLUMN email_recuperacao varchar(254);

ALTER TABLE public.usuarios
    ADD CONSTRAINT uk_usuarios_email_recuperacao
        UNIQUE (email_recuperacao),
    ADD CONSTRAINT ck_usuarios_email_recuperacao
        CHECK (
            email_recuperacao IS NULL
            OR (
                email_recuperacao = lower(btrim(email_recuperacao))
                AND email_recuperacao <> ''
            )
        );
