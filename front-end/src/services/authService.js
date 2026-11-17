import axios from 'axios'

export async function fazerLogin(login, senha) {
  const resposta = await axios.post(
    'http://localhost:9001/auth/login',
    { login, senha },
    { timeout: 10000 },
  )

  return resposta.data
}
