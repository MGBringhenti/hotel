export function criarSessao(dados) {
  const partes = dados.accessToken.split('.')

  if (partes.length !== 3) {
    throw new Error('Formato de token inválido.')
  }

  const base64 = partes[1].replace(/-/g, '+').replace(/_/g, '/')

  const base64Completo = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')

  const bytes = Uint8Array.from(atob(base64Completo), (caractere) => caractere.charCodeAt(0))

  const conteudo = JSON.parse(new TextDecoder().decode(bytes))

  const funcoesPermitidas = ['ADMIN', 'RECEPCIONISTA', 'CLIENTE']

  if (!funcoesPermitidas.includes(conteudo.funcao)) {
    throw new Error('Perfil de usuário inválido.')
  }

  if (!Number.isFinite(conteudo.exp) || conteudo.exp * 1000 <= Date.now()) {
    throw new Error('Token expirado ou sem validade.')
  }

  return {
    accessToken: dados.accessToken,
    tokenType: dados.tokenType,
    expiraEm: conteudo.exp * 1000,
    funcao: conteudo.funcao,
  }
}
