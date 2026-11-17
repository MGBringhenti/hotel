import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { usePagina } from '../hooks/usePagina.js'
import { api, mensagemErro } from '../lib/api.js'
import { acoesReserva, dataBR, moeda } from '../lib/formatacao.js'
import { Aviso, Cabecalho, Paginacao, Situacao, Vazio } from '../components/UI.jsx'
import EditarReserva from '../components/EditarReserva.jsx'

function NomeRegistro({ servico, caminho, sessao, formato, fallback }) {
  const [dado, setDado] = useState(null)
  useEffect(() => {
    const controller = new AbortController()
    api(servico, caminho, { sessao, signal: controller.signal })
      .then(setDado)
      .catch(() => {})
    return () => controller.abort()
  }, [servico, caminho, sessao])
  return <>{dado ? formato(dado) : fallback}</>
}
export default function Reservas({ sessao }) {
  const lista = usePagina('reservas', '/reservas', sessao)
  const [editor, setEditor] = useState(null)
  const [ocupado, setOcupado] = useState(false)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const equipe = sessao.funcao !== 'CLIENTE'
  async function alterar(reserva, status) {
    if (ocupado) return
    if (!window.confirm(`Alterar a reserva #${reserva.idReserva} para ${status.toLowerCase()}?`))
      return
    setOcupado(true)
    setErro('')
    setSucesso('')
    try {
      await api('reservas', `/reservas/${reserva.idReserva}/status`, {
        sessao,
        method: 'PATCH',
        data: { status, versao: reserva.versao },
      })
      setSucesso('Situação da reserva atualizada.')
    } catch (falha) {
      setErro(mensagemErro(falha, true))
    } finally {
      setOcupado(false)
      lista.recarregar()
    }
  }
  return (
    <>
      <Cabecalho
        titulo={equipe ? 'Reservas' : 'Minhas reservas'}
        descricao="Acompanhe estadias, datas e solicitações em um só lugar."
      >
        <button
          className="secondary"
          disabled={ocupado || lista.carregando}
          onClick={lista.recarregar}
        >
          Atualizar
        </button>
        <Link className="button" to="/disponibilidade">
          Nova reserva
        </Link>
      </Cabecalho>
      <Aviso erro={erro || lista.erro} sucesso={sucesso} />
      {lista.carregando ? (
        <p role="status" className="loading">
          Carregando reservas...
        </p>
      ) : !lista.pagina?.content.length ? (
        <Vazio>Nenhuma reserva por aqui. Escolha um quarto para planejar uma estadia.</Vazio>
      ) : (
        <div className="reservations">
          {lista.pagina.content.map((r) => (
            <article className="reservation" key={r.idReserva}>
              <header>
                <h2>Reserva #{r.idReserva}</h2>
                <Situacao valor={r.status} />
              </header>
              <h3>
                <NomeRegistro
                  servico="quartos"
                  caminho={`/quarto/${r.idQuarto}`}
                  sessao={sessao}
                  formato={(q) => `Quarto ${q.numero} · ${q.tipo.nome}`}
                  fallback="Quarto não disponível para consulta"
                />
              </h3>
              {equipe && (
                <p>
                  <NomeRegistro
                    servico="clientes"
                    caminho={`/clientes/${r.idCliente}`}
                    sessao={sessao}
                    formato={(c) => c.nome}
                    fallback="Cliente não disponível para consulta"
                  />
                </p>
              )}
              <dl className="reservation-dates">
                <div>
                  <dt>Entrada</dt>
                  <dd>{dataBR(r.dataInicio)}</dd>
                </div>
                <div>
                  <dt>Saída</dt>
                  <dd>{dataBR(r.dataFinal)}</dd>
                </div>
                <div>
                  <dt>Total</dt>
                  <dd>{moeda(r.valorTotal)}</dd>
                </div>
              </dl>
              <div className="actions">
                {equipe && r.status === 'PENDENTE' && (
                  <button disabled={ocupado} className="secondary" onClick={() => setEditor(r)}>
                    Editar
                  </button>
                )}
                {acoesReserva(r, sessao.funcao).map(([status, nome]) => (
                  <button
                    key={status}
                    disabled={ocupado}
                    className={status === 'CANCELADA' ? 'danger' : 'secondary'}
                    onClick={() => alterar(r, status)}
                  >
                    {nome}
                  </button>
                ))}
              </div>
            </article>
          ))}
        </div>
      )}
      <Paginacao
        pagina={lista.pagina}
        onChange={lista.irPara}
        disabled={ocupado || lista.carregando}
      />
      {editor && (
        <EditarReserva
          reserva={editor}
          sessao={sessao}
          onFechar={() => {
            setEditor(null)
            lista.recarregar()
          }}
          onSalva={() => {
            setEditor(null)
            setSucesso('Reserva editada com sucesso.')
            setErro('')
            lista.recarregar()
          }}
        />
      )}
    </>
  )
}
