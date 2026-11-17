export const moeda = (valor) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor)
export const dataBR = (valor) => (valor ? valor.split('-').reverse().join('/') : '—')
export function hojeHotel() {
  const partes = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Sao_Paulo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date())
  const obter = (tipo) => partes.find((p) => p.type === tipo).value
  return `${obter('year')}-${obter('month')}-${obter('day')}`
}
export function somarDias(data, quantidade) {
  const instante = new Date(`${data}T12:00:00Z`)
  instante.setUTCDate(instante.getUTCDate() + quantidade)
  return instante.toISOString().slice(0, 10)
}
export function acoesReserva(reserva, funcao, hoje = hojeHotel()) {
  const equipe = ['ADMIN', 'RECEPCIONISTA'].includes(funcao)
  const acoes = []
  if (equipe && reserva.status === 'PENDENTE' && hoje <= reserva.dataInicio)
    acoes.push(['CONFIRMADA', 'Confirmar'])
  if (['PENDENTE', 'CONFIRMADA'].includes(reserva.status) && hoje < reserva.dataInicio)
    acoes.push(['CANCELADA', 'Cancelar'])
  if (equipe && reserva.status === 'CONFIRMADA' && hoje >= reserva.dataFinal)
    acoes.push(['CONCLUIDA', 'Concluir'])
  return acoes
}
export const ocupadoEm = (data, periodos) =>
  periodos.some((p) => data >= p.dataInicio && data < p.dataFinal)
export function validarSenha(senha) {
  if (senha.length < 8 || senha.length > 50) return 'A senha deve ter entre 8 e 50 caracteres.'
  if (new TextEncoder().encode(senha).length > 72)
    return 'A senha ultrapassa o limite de 72 bytes. Use menos caracteres.'
  return ''
}
export const perfis = { ADMIN: 'Administrador', RECEPCIONISTA: 'Recepção', CLIENTE: 'Hóspede' }
