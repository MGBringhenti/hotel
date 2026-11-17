import { useEffect, useState } from 'react'
import { usePagina } from '../hooks/usePagina.js'
import { api } from '../lib/api.js'
import { Paginacao, Aviso } from './UI.jsx'
const configuracoes = {
  clientes: {
    servico: 'clientes',
    caminho: '/clientes',
    titulo: 'Cliente',
    nome: (c) => `${c.nome} — ${c.email}`,
    aceita: (c) => c.ativo,
  },
  quartos: {
    servico: 'quartos',
    caminho: '/quarto',
    titulo: 'Quarto',
    nome: (q) => `Quarto ${q.numero} · ${q.tipo.nome} · até ${q.tipo.capacidade} pessoas`,
    aceita: (q) => q.status === 'DISPONIVEL',
  },
  tipos: {
    servico: 'quartos',
    caminho: '/tipoQuarto',
    titulo: 'Tipo de quarto',
    nome: (t) => `${t.nome} · ${t.capacidade} pessoas`,
    aceita: () => true,
  },
  usuarios: {
    servico: 'clientes',
    caminho: '/usuarios',
    titulo: 'Usuário',
    nome: (u) => `${u.login} — ${u.emailRecuperacao || 'Sem e-mail'}`,
    aceita: (u) => u.ativo && u.funcao === 'CLIENTE',
  },
}
export default function Seletor({ tipo, sessao, valor, onChange, disabled = false }) {
  const config = configuracoes[tipo]
  const lista = usePagina(config.servico, config.caminho, sessao)
  const [busca, setBusca] = useState('')
  const [selecionado, setSelecionado] = useState(null)
  useEffect(() => {
    if (!valor) return
    const controller = new AbortController()
    api(config.servico, `${config.caminho}/${valor}`, { sessao, signal: controller.signal })
      .then(setSelecionado)
      .catch(() => {})
    return () => controller.abort()
  }, [config, sessao, valor])
  const nomeAtual =
    selecionado && String(selecionado.id) === String(valor)
      ? config.nome(selecionado)
      : valor
        ? 'Seleção atual mantida'
        : 'Nenhum selecionado'
  const itens = (lista.pagina?.content || []).filter((item) =>
    config.nome(item).toLocaleLowerCase('pt-BR').includes(busca.toLocaleLowerCase('pt-BR')),
  )
  return (
    <fieldset className="selector" disabled={disabled}>
      <legend>{config.titulo}</legend>
      <p className="selected-name">{nomeAtual}</p>
      <label className="field">
        <span>Filtrar nesta página</span>
        <input
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
          placeholder={`Nome do ${config.titulo.toLowerCase()}`}
        />
      </label>
      <Aviso erro={lista.erro} />
      {lista.carregando ? (
        <p role="status">Carregando opções...</p>
      ) : (
        <div className="selector-list">
          {itens.map((item) => (
            <button
              key={item.id}
              type="button"
              className={String(valor) === String(item.id) ? 'option selected' : 'option'}
              aria-pressed={String(valor) === String(item.id)}
              disabled={!config.aceita(item)}
              onClick={() => {
                setSelecionado(item)
                onChange(String(item.id), item)
              }}
            >
              {config.nome(item)}
              {!config.aceita(item) && ' (indisponível)'}
            </button>
          ))}
          {!itens.length && (
            <p>Nenhuma opção nesta página. Verifique as demais páginas ou cadastre um registro.</p>
          )}
        </div>
      )}
      <Paginacao pagina={lista.pagina} onChange={lista.irPara} disabled={lista.carregando} />
      {lista.erro && (
        <button type="button" className="secondary" onClick={lista.recarregar}>
          Tentar novamente
        </button>
      )}
    </fieldset>
  )
}
