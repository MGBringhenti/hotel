import axios from 'axios'

export async function listarQuartos(token, numeroPagina = 0) {
  const resposta = await axios.get('http://localhost:9002/quarto', {
    params: {
      pagina: numeroPagina,
      tamanho: 20,
    },
    headers: {
      Authorization: `Bearer ${token}`,
    },
    timeout: 10000,
  })

  return resposta.data
}
