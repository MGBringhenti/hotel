import { Buffer } from 'node:buffer'
import test from 'node:test'
import assert from 'node:assert/strict'
import { acoesReserva, ocupadoEm, somarDias, validarSenha } from '../src/lib/formatacao.js'
import { criarSessao } from '../src/services/sessao.js'
test('cliente só cancela antes da entrada', () => {
  const r = { status: 'PENDENTE', dataInicio: '2030-01-10', dataFinal: '2030-01-12' }
  assert.deepEqual(acoesReserva(r, 'CLIENTE', '2030-01-09'), [['CANCELADA', 'Cancelar']])
  assert.deepEqual(acoesReserva(r, 'CLIENTE', '2030-01-10'), [])
})
test('equipe confirma até entrada e conclui apenas na saída', () => {
  const r = { status: 'PENDENTE', dataInicio: '2030-01-10', dataFinal: '2030-01-12' }
  assert.deepEqual(acoesReserva(r, 'ADMIN', '2030-01-10'), [['CONFIRMADA', 'Confirmar']])
  assert.deepEqual(acoesReserva({ ...r, status: 'CONFIRMADA' }, 'RECEPCIONISTA', '2030-01-11'), [])
  assert.deepEqual(acoesReserva({ ...r, status: 'CONFIRMADA' }, 'RECEPCIONISTA', '2030-01-12'), [
    ['CONCLUIDA', 'Concluir'],
  ])
})
test('reservas encerradas não oferecem transições', () => {
  for (const status of ['CANCELADA', 'CONCLUIDA'])
    assert.deepEqual(acoesReserva({ status, dataInicio: '2030-01-10' }, 'ADMIN', '2030-01-09'), [])
})
test('calendário considera entrada inclusiva e saída exclusiva', () => {
  const periodos = [{ dataInicio: '2030-01-10', dataFinal: '2030-01-12' }]
  assert.equal(ocupadoEm('2030-01-10', periodos), true)
  assert.equal(ocupadoEm('2030-01-11', periodos), true)
  assert.equal(ocupadoEm('2030-01-12', periodos), false)
})
test('datas atravessam mês e ano sem fuso local', () => {
  assert.equal(somarDias('2030-12-31', 1), '2031-01-01')
  assert.equal(somarDias('2028-02-28', 1), '2028-02-29')
})
test('senha respeita bytes UTF-8 além da quantidade de caracteres', () => {
  assert.equal(validarSenha('Senha1234!'), '')
  assert.notEqual(validarSenha('curta'), '')
  assert.notEqual(validarSenha('á'.repeat(40)), '')
})
function token(claims) {
  return [
    'e30',
    Buffer.from(JSON.stringify(claims)).toString('base64url'),
    'assinatura-teste',
  ].join('.')
}
test('sessão aceita perfis esperados e extrai expiração', () => {
  const exp = Math.floor(Date.now() / 1000) + 60
  assert.equal(
    criarSessao({ accessToken: token({ funcao: 'CLIENTE', exp }), tokenType: 'Bearer' }).expiraEm,
    exp * 1000,
  )
})
test('sessão recusa token expirado, malformado e perfil desconhecido', () => {
  assert.throws(() => criarSessao({ accessToken: 'invalido' }))
  assert.throws(() => criarSessao({ accessToken: token({ funcao: 'ADMIN', exp: 1 }) }))
  assert.throws(() => criarSessao({ accessToken: token({ funcao: 'OUTRO', exp: 9999999999 }) }))
})
