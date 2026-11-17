import axios from 'axios'
const bases = {
  clientes: import.meta.env.VITE_CLIENTES_URL || 'http://localhost:9001',
  quartos: import.meta.env.VITE_QUARTOS_URL || 'http://localhost:9002',
  reservas: import.meta.env.VITE_RESERVAS_URL || 'http://localhost:9003',
}
export async function api(servico, caminho, { sessao, method = 'GET', data, params, signal } = {}) {
  if (sessao && Date.now() >= sessao.expiraEm) {
    window.dispatchEvent(new Event('hotel:sessao-expirada'))
    throw new Error('Sua sessão expirou. Entre novamente.')
  }
  try {
    const resposta = await axios({
      baseURL: bases[servico],
      url: caminho,
      method,
      data,
      params,
      signal,
      timeout: 15000,
      headers: sessao ? { Authorization: `Bearer ${sessao.accessToken}` } : {},
    })
    return resposta.data
  } catch (erro) {
    if (sessao && erro.response?.status === 401)
      window.dispatchEvent(new Event('hotel:sessao-expirada'))
    throw erro
  }
}
export function mensagemErro(erro, escrita = false) {
  if (erro.response?.status === 401)
    return 'Credenciais inválidas ou sessão expirada. Entre novamente.'
  if (erro.response?.status === 403) return 'Seu perfil não tem permissão para esta operação.'
  const detalhe = erro.response?.data?.mensagem
  if (erro.response?.status < 500 && typeof detalhe === 'string') return detalhe
  if (erro.response?.status === 404) return 'Registro não encontrado. Atualize a lista.'
  if (erro.response?.status === 409)
    return 'Os dados mudaram ou existe um conflito. Atualize a lista antes de tentar novamente.'
  if (!erro.response && !axios.isAxiosError(erro))
    return erro.message || 'Não foi possível concluir a operação.'
  return escrita
    ? 'Não foi possível confirmar o resultado. Atualize a lista antes de repetir a operação.'
    : 'Não foi possível consultar o serviço. Verifique se a API está iniciada e tente novamente.'
}
