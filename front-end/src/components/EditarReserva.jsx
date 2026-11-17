import { useState } from 'react'
import { api, mensagemErro } from '../lib/api.js'
import { hojeHotel } from '../lib/formatacao.js'
import Seletor from './Seletor.jsx'
import { Aviso, Campo, Modal } from './UI.jsx'

export default function EditarReserva({ reserva, sessao, onSalva, onFechar }) {
  const [cliente, setCliente] = useState(String(reserva.idCliente))
  const [quarto, setQuarto] = useState(String(reserva.idQuarto))
  const [inicio, setInicio] = useState(reserva.dataInicio)
  const [fim, setFim] = useState(reserva.dataFinal)
  const [ocupado, setOcupado] = useState(false)
  const [erro, setErro] = useState('')
  const [bloqueado, setBloqueado] = useState(false)
  async function salvar(e) {
    e.preventDefault()
    if (ocupado || bloqueado) return
    if (!cliente || !quarto || inicio < hojeHotel() || fim <= inicio) {
      setErro('Selecione cliente e quarto e confira o período da reserva.')
      return
    }
    setOcupado(true)
    setErro('')
    try {
      const atualizada = await api('reservas', `/reservas/${reserva.idReserva}`, {
        sessao,
        method: 'PUT',
        data: {
          idCliente: Number(cliente),
          idQuarto: Number(quarto),
          dataInicio: inicio,
          dataFinal: fim,
          versao: reserva.versao,
        },
      })
      onSalva(atualizada)
    } catch (falha) {
      setErro(mensagemErro(falha, true))
      if (falha.response?.status === 409 || !falha.response || falha.response?.status >= 500)
        setBloqueado(true)
    } finally {
      setOcupado(false)
    }
  }
  return (
    <Modal titulo={`Editar reserva #${reserva.idReserva}`} ocupado={ocupado} onFechar={onFechar}>
      <form onSubmit={salvar}>
        <fieldset disabled={ocupado || bloqueado} className="stack">
          <Seletor
            tipo="clientes"
            sessao={sessao}
            valor={cliente}
            onChange={setCliente}
            disabled={ocupado || bloqueado}
          />
          <Seletor
            tipo="quartos"
            sessao={sessao}
            valor={quarto}
            onChange={setQuarto}
            disabled={ocupado || bloqueado}
          />
          <div className="form-grid">
            <Campo
              label="Entrada"
              type="date"
              required
              min={hojeHotel()}
              value={inicio}
              onChange={(e) => setInicio(e.target.value)}
            />
            <Campo
              label="Saída"
              type="date"
              required
              min={inicio}
              value={fim}
              onChange={(e) => setFim(e.target.value)}
            />
          </div>
          <p className="muted">
            O total será recalculado. Ao trocar de quarto, será aplicada a diária atual do novo
            quarto.
          </p>
          <button>{ocupado ? 'Salvando...' : 'Salvar alterações'}</button>
        </fieldset>
      </form>
      <Aviso erro={erro} />
      {bloqueado && <p>Feche a edição e atualize a lista antes de tentar novamente.</p>}
    </Modal>
  )
}
