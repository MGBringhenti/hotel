import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api, mensagemErro } from '../lib/api.js'
import { Aviso, Campo } from '../components/UI.jsx'
export default function Login({ onLogin, aviso }) {
  const [login, setLogin] = useState('')
  const [senha, setSenha] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const [erro, setErro] = useState('')
  async function entrar(e) {
    e.preventDefault()
    if (ocupado) return
    setOcupado(true)
    setErro('')
    try {
      const dados = await api('clientes', '/auth/login', { method: 'POST', data: { login, senha } })
      onLogin(dados)
    } catch (falha) {
      setErro(falha.response?.status === 401 ? 'Login ou senha incorretos.' : mensagemErro(falha))
    } finally {
      setOcupado(false)
      setSenha('')
    }
  }
  return (
    <div className="auth-layout">
      <section className="auth-story">
        <div className="brand-light">
          Hotel<span>Estadias & hospitalidade</span>
        </div>
        <h1>
          O cuidado começa
          <br />
          antes da chegada.
        </h1>
        <p>Um espaço para planejar sua estadia e organizar cada detalhe da hospedagem.</p>
        <div className="door-art" aria-hidden="true">
          <span>H</span>
        </div>
      </section>
      <section className="auth-form">
        <h2>Boas-vindas</h2>
        <p>Entre com sua conta para continuar.</p>
        <Aviso erro={erro} sucesso={aviso} />
        <form onSubmit={entrar}>
          <fieldset disabled={ocupado} className="stack">
            <Campo
              label="Login"
              autoComplete="username"
              required
              value={login}
              onChange={(e) => setLogin(e.target.value)}
            />
            <Campo
              label="Senha"
              type="password"
              autoComplete="current-password"
              required
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
            />
            <button>{ocupado ? 'Entrando...' : 'Entrar'}</button>
          </fieldset>
        </form>
        <Link to="/recuperar-senha">Esqueci minha senha</Link>
        <p className="muted">Ainda não tem acesso? Solicite seu cadastro à recepção.</p>
      </section>
    </div>
  )
}
