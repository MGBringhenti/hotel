CREATE TABLE public.recuperacao_senha (
    id UUID PRIMARY KEY,
    usuario_id INTEGER NOT NULL,
    usuario_versao BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    utilizado_em TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_recuperacao_senha_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES public.usuarios(id),

    CONSTRAINT uk_recuperacao_senha_token
        UNIQUE (token_hash),

    CONSTRAINT ck_recuperacao_senha_versao
        CHECK (usuario_versao >= 0),

    CONSTRAINT ck_recuperacao_senha_expiracao
        CHECK (expira_em > criado_em),

    CONSTRAINT ck_recuperacao_senha_utilizacao
        CHECK (utilizado_em IS NULL OR utilizado_em >= criado_em)
);

CREATE INDEX idx_recuperacao_senha_usuario
    ON public.recuperacao_senha (usuario_id);