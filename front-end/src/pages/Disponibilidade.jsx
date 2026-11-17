import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import Seletor from '../components/Seletor.jsx'
import Calendario from '../components/Calendario.jsx'
import { Aviso, Cabecalho, Campo, Situacao } from '../components/UI.jsx'
import { api, mensagemErro } from '../lib/api.js'
import { dataBR, hojeHotel, moeda } from '../lib/formatacao.js'

export default function Disponibilidade({ sessao }) {
  const [params] = useSearchParams()
  const inicial = params.get('idQuarto')
  const [quarto, setQuarto] = useState(/^\d+$/.test(inicial || '') ? inicial : '')
  const [cliente, setCliente] = useState('')
  const [inicio, setInicio] = useState('')
  const [fim, setFim] = useState('')
  const [resultado, setResultado] = useState(null)
  const [criada, setCriada] = useState(null)
  const [ocupado, setOcupado] = useState(false)
  const [erro, setErro] = useState('')
  const ehCliente = sessao.funcao === 'CLIENTE'
  function limpar() {
    setResultado(null)
    setCriada(null)
    setErro('')
  }
  function selecionarDia(dia) {
    limpar()
    if (!inicio || fim || dia <= inicio) {
      setInicio(dia)
      setFim('')
    } else setFim(dia)
  }
  async function consultar(e) {
    e.preventDefault()
    if (ocupado) return
    limpar()
    if (!quarto) {
      setErro('Escolha um quarto.')
      return
    }
    if (!inicio || !fim || inicio < hojeHotel() || fim <= inicio) {
      setErro('Informe entrada a partir de hoje e saída posterior à entrada.')
      return
    }
    setOcupado(true)
    try {
      const dados = await api('reservas', '/reservas/disponibilidade', {
        sessao,
        params: { idQuarto: Number(quarto), dataInicio: inicio, dataFinal: fim },
      })
      setResultado(dados)
    } catch (falha) {
      setErro(mensagemErro(falha))
    } finally {
      setOcupado(false)
    }
  }
  async function reservar(e) {
    e.preventDefault()
    if (ocupado || !resultado?.disponivel) return
    if (!ehCliente && !cliente) {
      setErro('Selecione o cliente da reserva.')
      return
    }
    setOcupado(true)
    setErro('')
    const corpo = {
      idQuarto: resultado.idQuarto,
      dataInicio: resultado.dataInicio,
      dataFinal: resultado.dataFinal,
    }
    if (!ehCliente) corpo.idCliente = Number(cliente)
    try {
      const dados = await api('reservas', ehCliente ? '/reservas/minhas' : '/reservas', {
        sessao,
        method: 'POST',
        data: corpo,
      })
      setCriada(dados)
      setResultado(null)
    } catch (falha) {
      setResultado(null)
      setErro(mensagemErro(falha, true))
    } finally {
      setOcupado(false)
    }
  }
  return (
    <>
      <Cabecalho
        titulo="Planejar uma estadia"
        descricao="Escolha o quarto, confira as datas e reserve."
      />
      <Aviso erro={erro} />
      <div className="booking-layout">
        <div>
          <fieldset disabled={ocupado} className="plain-fieldset">
            <Seletor
              tipo="quartos"
              sessao={sessao}
              valor={quarto}
              disabled={ocupado}
              onChange={(v) => {
                setQuarto(v)
                limpar()
              }}
            />
            {quarto && (
              <Calendario
                key={quarto + (criada?.idReserva || '')}
                quarto={quarto}
                sessao={sessao}
                inicio={inicio}
                fim={fim}
                onDia={selecionarDia}
              />
            )}
          </fieldset>
        </div>
        <aside className="booking-summary">
          <h2>Sua estadia</h2>
          <form onSubmit={consultar}>
            <fieldset disabled={ocupado} className="stack">
              <Campo
                label="Entrada"
                type="date"
                required
                min={hojeHotel()}
                value={inicio}
                onChange={(e) => {
                  setInicio(e.target.value)
                  limpar()
                }}
              />
              <Campo
                label="Saída"
                type="date"
                required
                min={inicio || hojeHotel()}
                value={fim}
                onChange={(e) => {
                  setFim(e.target.value)
                  limpar()
                }}
              />
              <button disabled={!quarto}>
                {ocupado ? 'Aguarde...' : 'Consultar disponibilidade'}
              </button>
            </fieldset>
          </form>
          {resultado && (
            <div className="aviso" role="status">
              {resultado.disponivel
                ? 'Quarto disponível para este período.'
                : 'Quarto indisponível. Escolha outras datas ou outro quarto.'}
            </div>
          )}
          {resultado?.disponivel && (
            <form onSubmit={reservar}>
              <fieldset disabled={ocupado} className="stack">
                {!ehCliente && (
                  <Seletor
                    tipo="clientes"
                    sessao={sessao}
                    valor={cliente}
                    onChange={setCliente}
                    disabled={ocupado}
                  />
                )}
                <p>
                  Entrada em {dataBR(resultado.dataInicio)} e saída em {dataBR(resultado.dataFinal)}
                  .
                </p>
                <p className="muted">
                  A reserva será criada como pendente. O total definitivo será apresentado após o
                  cadastro.
                </p>
                <button>{ocupado ? 'Reservando...' : 'Criar reserva'}</button>
              </fieldset>
            </form>
          )}
          {criada && (
            <div className="reservation-success" role="status">
              <h3>Reserva #{criada.idReserva} criada</h3>
              <Situacao valor={criada.status} />
              <p className="price">{moeda(criada.valorTotal)}</p>
              <Link className="button" to="/reservas">
                Ver minhas reservas
              </Link>
            </div>
          )}
        </aside>
      </div>
    </>
  )
}
