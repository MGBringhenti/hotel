import { useEffect, useState } from 'react'
import { api, mensagemErro } from '../lib/api.js'
import { hojeHotel, ocupadoEm, somarDias } from '../lib/formatacao.js'
import { Aviso } from './UI.jsx'

function Mes({ mes, quarto, sessao, inicio, fim, onDia }) {
  const [resultado, setResultado] = useState(null)
  const [erro, setErro] = useState('')
  const [revisao, setRevisao] = useState(0)
  const primeiro = mes + '-01'
  const proximo = new Date(primeiro + 'T12:00:00Z')
  proximo.setUTCMonth(proximo.getUTCMonth() + 1)
  const limite = proximo.toISOString().slice(0, 10)
  const inicioConsulta = primeiro < hojeHotel() ? hojeHotel() : primeiro
  useEffect(() => {
    const controller = new AbortController()
    api('reservas', '/reservas/calendario', {
      sessao,
      signal: controller.signal,
      params: { idQuarto: quarto, dataInicio: inicioConsulta, dataFinal: limite },
    })
      .then(setResultado)
      .catch((falha) => {
        if (!controller.signal.aborted) setErro(mensagemErro(falha))
      })
    return () => controller.abort()
  }, [quarto, sessao, inicioConsulta, limite, revisao])
  const dias = []
  for (let dia = primeiro; dia < limite; dia = somarDias(dia, 1)) dias.push(dia)
  const deslocamento = new Date(primeiro + 'T12:00:00Z').getUTCDay()
  return (
    <>
      <Aviso erro={erro} />
      {erro && (
        <button
          type="button"
          className="secondary"
          onClick={() => {
            setErro('')
            setRevisao((v) => v + 1)
          }}
        >
          Tentar novamente
        </button>
      )}
      {!resultado && !erro && <p role="status">Consultando o calendário...</p>}
      {resultado && (
        <>
          {!resultado.quartoAtivo && (
            <p role="alert">Este quarto está inativo e não aceita novas reservas.</p>
          )}
          <div className="calendar-grid">
            {['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'].map((d) => (
              <span className="weekday" key={d}>
                {d}
              </span>
            ))}
            {Array.from({ length: deslocamento }, (_, i) => (
              <span key={`vazio-${i}`} />
            ))}
            {dias.map((dia) => {
              const ocupado = ocupadoEm(dia, resultado.periodosOcupados)
              const passado = dia < hojeHotel()
              const selecionado = dia === inicio || dia === fim
              const intervalo = inicio && fim && dia > inicio && dia < fim
              return (
                <button
                  type="button"
                  key={dia}
                  className={`calendar-day ${ocupado ? 'booked' : 'free'} ${selecionado ? 'chosen' : ''} ${intervalo ? 'between' : ''}`}
                  disabled={
                    passado ||
                    !resultado.quartoAtivo ||
                    (ocupado && !(inicio && !fim && dia > inicio))
                  }
                  aria-label={`${dia.split('-').reverse().join('/')} — ${ocupado ? 'ocupado' : 'livre'}`}
                  aria-pressed={selecionado}
                  onClick={() => onDia(dia)}
                >
                  {Number(dia.slice(-2))}
                  <small>{passado ? 'Passado' : ocupado ? 'Ocupado' : 'Livre'}</small>
                </button>
              )
            })}
          </div>
          <p className="muted">
            Selecione a entrada e depois a saída. A data de saída não conta como diária. A
            disponibilidade será conferida novamente antes da reserva.
          </p>
        </>
      )}
    </>
  )
}
export default function Calendario({ quarto, sessao, inicio, fim, onDia }) {
  const [mes, setMes] = useState(hojeHotel().slice(0, 7))
  return (
    <section className="calendar">
      <div className="calendar-header">
        <h3>Calendário do quarto</h3>
        <label className="field">
          <span>Mês</span>
          <input
            type="month"
            min={hojeHotel().slice(0, 7)}
            required
            value={mes}
            onChange={(e) => {
              if (e.target.value) setMes(e.target.value)
            }}
          />
        </label>
      </div>
      <Mes
        key={quarto + mes}
        mes={mes}
        quarto={quarto}
        sessao={sessao}
        inicio={inicio}
        fim={fim}
        onDia={onDia}
      />
    </section>
  )
}
