import { hojeHotel } from '../../src/lib/formatacao.js'
import { test, expect } from '@playwright/test'
import { Buffer } from 'node:buffer'

function jwt(funcao) {
  return [
    'e30',
    Buffer.from(
      JSON.stringify({ funcao, exp: Math.floor(Date.now() / 1000) + 7200, sub: '1', clienteId: 1 }),
    ).toString('base64url'),
    'teste',
  ].join('.')
}
async function preparar(page, perfil = 'ADMIN') {
  const requisicoes = []
  const tipo = { id: 1, nome: 'Suíte jardim', capacidade: 2, preco: 340, versao: 0 }
  const quarto = { id: 1, numero: 101, status: 'DISPONIVEL', tipo, versao: 0 }
  const cliente = {
    id: 1,
    nome: 'Ana Silva',
    email: 'ana@example.test',
    cpf: '52998224725',
    telefone: '11999999999',
    ativo: true,
    versao: 0,
  }
  const usuario = {
    id: 1,
    login: 'ana',
    emailRecuperacao: 'ana@example.test',
    funcao: 'CLIENTE',
    ativo: true,
    versao: 0,
  }
  let reservas = [
    {
      idReserva: 1,
      idCliente: 1,
      idQuarto: 1,
      dataInicio: '2030-01-10',
      dataFinal: '2030-01-12',
      valorTotal: 680,
      precoDiarioAplicado: 340,
      status: 'PENDENTE',
      versao: 0,
    },
  ]
  const colecoes = {
    '/clientes': [cliente],
    '/usuarios': [usuario],
    '/quarto': [quarto],
    '/tipoQuarto': [tipo],
  }
  const paginar = (itens, numero = 0) => ({
    content: itens,
    totalElements: itens.length,
    number: numero,
    totalPages: 1,
    first: true,
    last: true,
  })
  await page.route(/http:\/\/localhost:900[123]\//, async (route) => {
    const req = route.request()
    const url = new URL(req.url())
    const path = url.pathname
    const method = req.method()
    const body = req.postData() ? req.postDataJSON() : undefined
    requisicoes.push({
      path,
      method,
      body,
      query: Object.fromEntries(url.searchParams),
      headers: req.headers(),
    })
    const responder = (json, status = 200) =>
      route.fulfill({ status, json, headers: { 'access-control-allow-origin': '*' } })
    if (method === 'OPTIONS')
      return route.fulfill({
        status: 204,
        headers: {
          'access-control-allow-origin': '*',
          'access-control-allow-methods': '*',
          'access-control-allow-headers': '*',
        },
      })
    if (path === '/auth/login')
      return responder({ accessToken: jwt(perfil), tokenType: 'Bearer', expiresIn: 7200 })
    if (path.startsWith('/auth/')) return responder({})
    if (path === '/reservas/calendario')
      return responder({
        idQuarto: 1,
        quartoAtivo: true,
        dataInicio: url.searchParams.get('dataInicio'),
        dataFinal: url.searchParams.get('dataFinal'),
        periodosOcupados: [{ dataInicio: '2030-01-20', dataFinal: '2030-01-22' }],
      })
    if (path === '/reservas/disponibilidade')
      return responder({
        idQuarto: 1,
        dataInicio: url.searchParams.get('dataInicio'),
        dataFinal: url.searchParams.get('dataFinal'),
        disponivel: true,
      })
    if (path === '/reservas' && method === 'GET') return responder(paginar(reservas))
    if ((path === '/reservas' || path === '/reservas/minhas') && method === 'POST') {
      const nova = { ...reservas[0], ...body, idReserva: 2 }
      reservas.push(nova)
      return responder(nova, 201)
    }
    if (/^\/reservas\/\d+/.test(path)) {
      const r = reservas.find((item) => item.idReserva === Number(path.split('/')[2]))
      Object.assign(r, body, { versao: r.versao + 1 })
      return responder(r)
    }
    for (const [base, itens] of Object.entries(colecoes)) {
      if (path === base && method === 'GET') return responder(paginar(itens))
      if (path === base && method === 'POST') {
        const novo = { ...itens[0], ...body, id: 2 }
        itens.push(novo)
        return responder(novo, 201)
      }
      if (path.startsWith(base + '/')) {
        if (method === 'GET') return responder(itens[0])
        if (method === 'DELETE') {
          itens.splice(0, 1)
          return responder({})
        }
        Object.assign(itens[0], body, { versao: itens[0].versao + 1 })
        return responder(itens[0])
      }
    }
    return responder({ mensagem: 'Endpoint não simulado: ' + path }, 404)
  })
  await page.goto('/login')
  return requisicoes
}
async function entrar(page) {
  await page.getByLabel('Login', { exact: true }).fill('admin')
  await page.getByLabel('Senha', { exact: true }).fill('Senha1234!')
  await page.getByRole('button', { name: 'Entrar', exact: true }).click()
  await expect(page.getByRole('navigation', { name: 'Menu principal' })).toBeVisible()
}
test('admin: navegação, cadastro de cliente, edição e vínculo', async ({ page }) => {
  const requests = await preparar(page)
  await entrar(page)
  await page.getByRole('link', { name: 'Clientes', exact: true }).click()
  await page.getByRole('button', { name: 'Cadastrar cliente', exact: true }).click()
  await page.getByLabel('Nome completo').fill('Bruno Costa')
  await page.getByLabel('CPF', { exact: true }).fill('529.982.247-25')
  await page.getByLabel('E-mail', { exact: true }).fill('bruno@example.test')
  await page.getByLabel('Telefone').fill('11988888888')
  await page.getByRole('button', { name: 'Salvar', exact: true }).click()
  await expect(page.getByRole('status').filter({ hasText: 'Cadastro salvo' })).toBeVisible()
  expect(requests.find((r) => r.path === '/clientes' && r.method === 'POST').body.cpf).toBe(
    '52998224725',
  )
  await page.getByRole('button', { name: 'Editar', exact: true }).first().click()
  await page.getByLabel('Nome completo').fill('Ana Souza')
  await page.getByRole('button', { name: 'Salvar', exact: true }).click()
  await expect(page.getByText('Ana Souza', { exact: true })).toBeVisible()
  expect(requests.find((r) => r.path === '/clientes/1' && r.method === 'PUT').body.versao).toBe(0)
  await page.getByRole('button', { name: 'Vincular usuário' }).first().click()
  await page.getByRole('button', { name: 'ana — ana@example.test' }).click()
  page.once('dialog', (d) => d.accept())
  await page.getByRole('button', { name: 'Vincular conta', exact: true }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  expect(requests.find((r) => r.path === '/clientes/1/usuario').body).toEqual({ usuarioId: 1 })
})
test('cliente: criação pela rota própria, calendário e cancelamento', async ({ page }) => {
  const requests = await preparar(page, 'CLIENTE')
  await entrar(page)
  await expect(page.getByRole('link', { name: 'Usuários', exact: true })).toHaveCount(0)
  await page.getByRole('link', { name: 'Disponibilidade', exact: true }).click()
  await page.getByRole('button', { name: /Quarto 101/ }).click()
  await page.getByLabel('Mês', { exact: true }).fill('2030-01')
  await expect(page.getByRole('button', { name: '20/01/2030 — ocupado' })).toBeDisabled()
  await page.getByRole('button', { name: '10/01/2030 — livre' }).click()
  await page.getByRole('button', { name: '12/01/2030 — livre' }).click()
  await page.getByRole('button', { name: 'Consultar disponibilidade' }).click()
  await page.getByRole('button', { name: 'Criar reserva', exact: true }).click()
  await expect(page.getByText('Reserva #2 criada')).toBeVisible()
  const post = requests.find((r) => r.path === '/reservas/minhas' && r.method === 'POST')
  expect(post.body).toEqual({ idQuarto: 1, dataInicio: '2030-01-10', dataFinal: '2030-01-12' })
  expect(post.headers.authorization).toMatch(/^Bearer /)
  await page.getByRole('link', { name: 'Minhas reservas', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Confirmar', exact: true })).toHaveCount(0)
  page.once('dialog', (d) => d.accept())
  await page.getByRole('button', { name: 'Cancelar', exact: true }).first().click()
  await expect(page.getByText('CANCELADA', { exact: true })).toBeVisible()
  expect(requests.find((r) => r.path === '/reservas/1/status').body).toEqual({
    status: 'CANCELADA',
    versao: 0,
  })
})
test('recepção: cria em nome do cliente e não acessa usuários', async ({ page }) => {
  const requests = await preparar(page, 'RECEPCIONISTA')
  await entrar(page)
  await expect(page.getByRole('link', { name: 'Usuários', exact: true })).toHaveCount(0)
  await page.getByRole('link', { name: 'Quartos', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Inativar', exact: true })).toHaveCount(0)
  await page.getByRole('link', { name: 'Ver datas' }).click()
  await page.getByLabel('Entrada', { exact: true }).fill('2030-02-10')
  await page.getByLabel('Saída', { exact: true }).fill('2030-02-12')
  await page.getByRole('button', { name: 'Consultar disponibilidade' }).click()
  await page.getByRole('button', { name: 'Ana Silva — ana@example.test' }).click()
  await page.getByRole('button', { name: 'Criar reserva', exact: true }).click()
  await expect(page.getByText('Reserva #2 criada')).toBeVisible()
  expect(requests.find((r) => r.path === '/reservas' && r.method === 'POST').body.idCliente).toBe(1)
})
test('recuperação e redefinição usam contratos reais', async ({ page }) => {
  const requests = await preparar(page)
  await page.getByRole('link', { name: 'Esqueci minha senha' }).click()
  await page.getByLabel('E-mail de recuperação').fill('ana@example.test')
  await page.getByRole('button', { name: 'Enviar instruções' }).click()
  await expect(page.getByRole('status')).toContainText('Se houver uma conta')
  await page.getByRole('link', { name: 'Já tenho o código' }).click()
  await page.getByLabel('Código recebido por e-mail').fill('codigo-de-teste')
  await page.getByLabel('Nova senha', { exact: true }).fill('NovaSenha123!')
  await page.getByLabel('Confirmar nova senha').fill('NovaSenha123!')
  await page.getByRole('button', { name: 'Salvar nova senha' }).click()
  await expect(page.getByRole('status')).toContainText('Senha redefinida')
  expect(requests.find((r) => r.path.endsWith('/redefinir')).body).toEqual({
    token: 'codigo-de-teste',
    senhaNova: 'NovaSenha123!',
  })
})
test('edição de reserva envia versão e seleciona por nome', async ({ page }) => {
  const requests = await preparar(page)
  await entrar(page)
  await page.getByRole('link', { name: 'Reservas', exact: true }).click()
  await page.getByRole('button', { name: 'Editar', exact: true }).click()
  await page.getByLabel('Saída', { exact: true }).fill('2030-01-13')
  await page.getByRole('button', { name: 'Salvar alterações' }).click()
  await expect(page.getByText('Reserva editada com sucesso.')).toBeVisible()
  expect(requests.find((r) => r.path === '/reservas/1' && r.method === 'PUT').body).toEqual({
    idCliente: 1,
    idQuarto: 1,
    dataInicio: '2030-01-10',
    dataFinal: '2030-01-13',
    versao: 0,
  })
})
test('conflito de edição impede reenvio com versão antiga', async ({ page }) => {
  await preparar(page)
  await page.route('**/reservas/1', (route) =>
    route.fulfill({ status: 409, json: { mensagem: 'Versão desatualizada.' } }),
  )
  await entrar(page)
  await page.getByRole('link', { name: 'Reservas', exact: true }).click()
  await page.getByRole('button', { name: 'Editar', exact: true }).click()
  await page.getByRole('button', { name: 'Salvar alterações' }).click()
  await expect(page.getByRole('alert')).toContainText('Versão desatualizada')
  await expect(page.getByRole('button', { name: 'Salvar alterações' })).toBeDisabled()
  await page.getByRole('button', { name: 'Fechar janela' }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
})
test('401 encerra a sessão e não deixa página em branco', async ({ page }) => {
  await preparar(page)
  await entrar(page)
  await page.route('**/reservas?*', (route) => route.fulfill({ status: 401, json: {} }))
  await page.getByRole('link', { name: 'Reservas', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Boas-vindas' })).toBeVisible()
  await expect(page.getByRole('status')).toContainText('sessão expirou')
})
test('troca de senha exige confirmação e envia PATCH', async ({ page }) => {
  const requests = await preparar(page)
  await entrar(page)
  await page.getByRole('link', { name: 'Minha conta', exact: true }).click()
  await page.getByLabel('Senha atual').fill('Senha1234!')
  await page.getByLabel('Nova senha', { exact: true }).fill('OutraSenha123!')
  await page.getByLabel('Confirmar nova senha').fill('Diferente123!')
  await page.getByRole('button', { name: 'Salvar nova senha' }).click()
  await expect(page.getByRole('alert')).toContainText('confirmação')
  await page.getByLabel('Confirmar nova senha').fill('OutraSenha123!')
  await page.getByRole('button', { name: 'Salvar nova senha' }).click()
  await expect(page.getByRole('status')).toContainText('Senha alterada')
  expect(requests.find((r) => r.path === '/auth/senha').method).toBe('PATCH')
})
test('quartos e tipos: cadastro, situação e exclusão', async ({ page }) => {
  const requests = await preparar(page)
  await entrar(page)
  await page.getByRole('link', { name: 'Quartos', exact: true }).click()
  await page.getByRole('button', { name: 'Cadastrar quarto', exact: true }).click()
  await page.getByLabel('Número do quarto').fill('202')
  await page.getByRole('button', { name: 'Suíte jardim · 2 pessoas' }).click()
  await page.getByRole('button', { name: 'Salvar', exact: true }).click()
  await expect(page.getByRole('status').filter({ hasText: 'Cadastro salvo' })).toBeVisible()
  expect(requests.find((r) => r.path === '/quarto' && r.method === 'POST').body).toEqual({
    numero: 202,
    idTipo: 1,
  })
  page.once('dialog', (d) => d.accept())
  await page.getByRole('button', { name: 'Inativar', exact: true }).first().click()
  await expect(page.getByText('INATIVO', { exact: true })).toBeVisible()
  await page.getByRole('link', { name: 'Tipos de quarto', exact: true }).click()
  page.once('dialog', (d) => d.accept())
  await page.getByRole('button', { name: 'Excluir', exact: true }).click()
  await expect(page.getByRole('status').filter({ hasText: 'excluído' })).toBeVisible()
})
test('usuário: cadastro com perfil e edição sem senha', async ({ page }) => {
  const requests = await preparar(page)
  await entrar(page)
  await page.getByRole('link', { name: 'Usuários', exact: true }).click()
  await page.getByRole('button', { name: 'Cadastrar usuário', exact: true }).click()
  await page.getByLabel('Login', { exact: true }).fill('bruno')
  await page.getByLabel('E-mail de recuperação').fill('bruno@example.test')
  await page.getByLabel('Senha inicial').fill('SenhaInicial123!')
  await page.getByLabel('Perfil', { exact: true }).selectOption('RECEPCIONISTA')
  await page.getByRole('button', { name: 'Salvar', exact: true }).click()
  await expect(page.getByRole('status').filter({ hasText: 'Cadastro salvo' })).toBeVisible()
  expect(requests.find((r) => r.path === '/usuarios' && r.method === 'POST').body.funcao).toBe(
    'RECEPCIONISTA',
  )
  await page.getByRole('button', { name: 'Editar', exact: true }).first().click()
  await expect(page.getByLabel('Senha inicial')).toHaveCount(0)
  await page.getByLabel('Login', { exact: true }).fill('ana.novo')
  await page.getByRole('button', { name: 'Salvar', exact: true }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  expect(requests.find((r) => r.path === '/usuarios/1' && r.method === 'PUT').body).toEqual({
    login: 'ana.novo',
    emailRecuperacao: 'ana@example.test',
    versao: 0,
  })
})
test('responsivo e captura visual sem dados reais', async ({ page }, testInfo) => {
  await preparar(page)
  await page.screenshot({ path: testInfo.outputPath('login-desktop.png'), fullPage: true })
  await entrar(page)
  await page.screenshot({ path: testInfo.outputPath('inicio-desktop.png'), fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 })
  await page.getByRole('link', { name: 'Disponibilidade', exact: true }).click()
  await page.getByRole('button', { name: /Quarto 101/ }).click()
  await expect(page.getByText('Calendário do quarto')).toBeVisible()
  await expect(page.locator('.calendar-grid')).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(
    true,
  )
  await page.screenshot({ path: testInfo.outputPath('disponibilidade-mobile.png'), fullPage: true })
})

test('seletores acessam a segunda página sem perder a escolha', async ({ page }) => {
  await preparar(page, 'CLIENTE')
  await page.route('**/quarto?*', (route) => {
    const pagina = Number(new URL(route.request().url()).searchParams.get('pagina'))
    const q = {
      id: pagina + 1,
      numero: pagina ? 202 : 101,
      status: 'DISPONIVEL',
      tipo: { nome: 'Suíte', capacidade: 2, preco: 340 },
    }
    return route.fulfill({
      json: {
        content: [q],
        number: pagina,
        totalPages: 2,
        totalElements: 2,
        first: pagina === 0,
        last: pagina === 1,
      },
    })
  })
  await page.route('**/quarto/2', (route) =>
    route.fulfill({
      json: {
        id: 2,
        numero: 202,
        status: 'DISPONIVEL',
        tipo: { nome: 'Suíte', capacidade: 2, preco: 340 },
      },
    }),
  )
  await entrar(page)
  await page.getByRole('link', { name: 'Disponibilidade', exact: true }).click()
  await page.getByRole('button', { name: 'Próxima', exact: true }).click()
  await page.getByRole('button', { name: /Quarto 202/ }).click()
  await page.getByRole('button', { name: 'Anterior', exact: true }).click()
  await expect(page.locator('.selected-name')).toContainText('Quarto 202')
  await expect(page.getByRole('button', { name: /Quarto 101/ })).toBeVisible()
})
test('falha de rede ao reservar exige conferir a lista antes de repetir', async ({ page }) => {
  await preparar(page, 'CLIENTE')
  await page.route('**/reservas/minhas', (route) => route.abort('failed'))
  await entrar(page)
  await page.getByRole('link', { name: 'Disponibilidade', exact: true }).click()
  await page.getByRole('button', { name: /Quarto 101/ }).click()
  await page.getByLabel('Entrada', { exact: true }).fill('2030-02-10')
  await page.getByLabel('Saída', { exact: true }).fill('2030-02-12')
  await page.getByRole('button', { name: 'Consultar disponibilidade' }).click()
  await page.getByRole('button', { name: 'Criar reserva', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('confirmar o resultado')
  await expect(page.getByRole('button', { name: 'Criar reserva', exact: true })).toHaveCount(0)
})
test('rota administrativa não é exposta ao cliente', async ({ page }) => {
  const requests = await preparar(page, 'CLIENTE')
  await entrar(page)
  await page.evaluate(() => {
    window.history.pushState({}, '', '/usuarios')
    window.dispatchEvent(new PopStateEvent('popstate'))
  })
  await expect(page.getByRole('heading', { name: 'Página indisponível' })).toBeVisible()
  expect(requests.some((r) => r.path === '/usuarios')).toBe(false)
})
test('calendário do mês corrente consulta apenas de hoje em diante', async ({ page }) => {
  const requests = await preparar(page, 'CLIENTE')
  await entrar(page)
  await page.getByRole('link', { name: 'Disponibilidade', exact: true }).click()
  await page.getByRole('button', { name: /Quarto 101/ }).click()
  await expect(page.locator('.calendar-grid')).toBeVisible()
  expect(requests.some((r) => r.path === '/reservas/calendario')).toBe(true)
  expect(requests.find((r) => r.path === '/reservas/calendario').query.dataInicio).toBe(hojeHotel())
})
