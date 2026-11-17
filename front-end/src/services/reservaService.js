import axios from 'axios'

export async function listarReservas(token, numeroPagina = 0) {
  const resposta = await axios.get('http://localhost:9003/reservas', {
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

export async function alterarStatusReserva(token, idReserva, status, versao) {
  const resposta = await axios.patch(
    `http://localhost:9003/reservas/${idReserva}/status`,
    {
      status,
      versao,
    },
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
      timeout: 10000,
    },
  )

  return resposta.data
}

export async function criarReserva(token, funcao, dados) {
  const cliente = funcao === 'CLIENTE'

  if (!cliente && funcao !== 'ADMIN' && funcao !== 'RECEPCIONISTA') {
    throw new Error('Perfil não permitido.')
  }

  const url = cliente ? 'http://localhost:9003/reservas/minhas' : 'http://localhost:9003/reservas'

  const corpo = {
    idQuarto: dados.idQuarto,
    dataInicio: dados.dataInicio,
    dataFinal: dados.dataFinal,
  }

  if (!cliente) {
    corpo.idCliente = dados.idCliente
  }

  const resposta = await axios.post(url, corpo, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    timeout: 10000,
  })

  return resposta.data
}

export async function consultarDisponibilidade(token, idQuarto, dataInicio, dataFinal) {
  const resposta = await axios.get('http://localhost:9003/reservas/disponibilidade', {
    params: {
      idQuarto,
      dataInicio,
      dataFinal,
    },
    headers: {
      Authorization: `Bearer ${token}`,
    },
    timeout: 10000,
  })

  return resposta.data
}

export async function atualizarReserva(token, idReserva, dados) {
  const resposta = await axios.put(`http://localhost:9003/reservas/${idReserva}`, dados, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    timeout: 10000,
  })

  return resposta.data
}
