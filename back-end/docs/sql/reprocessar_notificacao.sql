-- Executar no banco hotel_notificacoes.
-- Script operacional: nao colocar em db/migration nem executar pelo Flyway.
-- Execute primeiro apenas a consulta abaixo.

SELECT
    n.evento_id,
    e.reserva_id,
    e.reserva_versao,
    n.tentativas,
    n.descartado_em,
    n.motivo_descarte
FROM public.notificacao n
JOIN public.evento_recebido e
    ON e.evento_id = n.evento_id
WHERE n.enviado_em IS NULL
  AND n.descartado_em IS NOT NULL
  AND n.motivo_descarte = 'LIMITE_TENTATIVAS'
ORDER BY n.descartado_em DESC;


-- Execute separadamente o UPDATE, somente depois de corrigir a causa da falha.
-- Substitua o UUID zerado pelo evento_id consultado acima.
-- O marcador zerado e bloqueado explicitamente para evitar uso acidental.
-- O contador sera zerado: as tentativas anteriores nao ficam somadas nele.
-- Nenhuma linha retornada significa que nenhuma notificacao foi reagendada.
-- O envio sera retomado pelo agendador quando estiver rodando e habilitado.
-- Se o e-mail foi entregue antes de uma falha na gravacao do sucesso,
-- o reprocessamento pode gerar um e-mail duplicado.

WITH alvo AS (
    SELECT '00000000-0000-0000-0000-000000000000'::uuid AS evento_id
)
UPDATE public.notificacao AS n
SET
    tentativas = 0,
    descartado_em = NULL,
    motivo_descarte = NULL,
    proxima_tentativa_em = CURRENT_TIMESTAMP
FROM public.evento_recebido AS e, alvo
WHERE n.evento_id = alvo.evento_id
  AND alvo.evento_id <> '00000000-0000-0000-0000-000000000000'::uuid
  AND e.evento_id = n.evento_id
  AND n.enviado_em IS NULL
  AND n.descartado_em IS NOT NULL
  AND n.motivo_descarte = 'LIMITE_TENTATIVAS'
  AND NOT EXISTS (
      SELECT 1
      FROM public.evento_recebido AS mais_recente
      WHERE mais_recente.reserva_id = e.reserva_id
        AND mais_recente.reserva_versao > e.reserva_versao
  )
RETURNING
    n.evento_id,
    n.tentativas,
    n.proxima_tentativa_em;
