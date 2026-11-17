import { useEffect, useState } from 'react'
import { Link, NavLink, Navigate, Route, Routes } from 'react-router-dom'
import Inicio from './pages/Inicio.jsx'
import Login from './pages/Login.jsx'
import Reservas from './pages/Reservas.jsx'
import Disponibilidade from './pages/Disponibilidade.jsx'
import Cadastros from './pages/Cadastros.jsx'
import Senha from './pages/Senha.jsx'
import { criarSessao } from './services/sessao.js'
import { perfis } from './lib/formatacao.js'
export default function App() {
  const [sessao, setSessao] = useState(null)
  const [aviso, setAviso] = useState('')
  useEffect(() => {
    function expirar() {
      setSessao(null)
      setAviso('Sua sessão expirou. Entre novamente para continuar.')
    }
    window.addEventListener('hotel:sessao-expirada', expirar)
    const timer = sessao ? setTimeout(expirar, Math.max(0, sessao.expiraEm - Date.now())) : null
    return () => {
      window.removeEventListener('hotel:sessao-expirada', expirar)
      clearTimeout(timer)
    }
  }, [sessao])
  function entrar(dados) {
    setSessao(criarSessao(dados))
    setAviso('')
  }
  function sair() {
    setSessao(null)
    setAviso('Você saiu da sua conta.')
  }
  if (!sessao)
    return (
      <Routes>
        <Route path="/login" element={<Login onLogin={entrar} aviso={aviso} />} />
        <Route path="/recuperar-senha" element={<Senha key="recuperar" modo="recuperar" />} />
        <Route path="/redefinir-senha" element={<Senha key="redefinir" modo="redefinir" />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    )
  const equipe = sessao.funcao !== 'CLIENTE'
  const admin = sessao.funcao === 'ADMIN'
  return (
    <div className="app-shell">
      <a className="skip-link" href="#conteudo">
        Pular para o conteúdo
      </a>
      <aside className="sidebar">
        <Link to="/" className="brand">
          Hotel<span>Estadias & hospitalidade</span>
        </Link>
        <nav aria-label="Menu principal">
          <NavLink end to="/">
            Início
          </NavLink>
          <NavLink to="/disponibilidade">Disponibilidade</NavLink>
          <NavLink to="/reservas">{equipe ? 'Reservas' : 'Minhas reservas'}</NavLink>
          <NavLink to="/quartos">Quartos</NavLink>
          {equipe && (
            <>
              <span className="nav-group">Cadastros</span>
              <NavLink to="/clientes">Clientes</NavLink>
              <NavLink to="/tipos">Tipos de quarto</NavLink>
            </>
          )}
          {admin && <NavLink to="/usuarios">Usuários</NavLink>}
          <span className="nav-group">Seu acesso</span>
          <NavLink to="/conta">Minha conta</NavLink>
        </nav>
        <div className="sidebar-footer">
          <span>{perfis[sessao.funcao]}</span>
          <button onClick={sair} className="logout">
            Sair da conta
          </button>
        </div>
      </aside>
      <div className="main-column">
        <header className="topbar">
          <span>Bem-vindo ao Hotel</span>
          <span>{perfis[sessao.funcao]}</span>
        </header>
        <main id="conteudo" tabIndex={-1}>
          <Routes>
            <Route path="/" element={<Inicio sessao={sessao} />} />
            <Route path="/reservas" element={<Reservas sessao={sessao} />} />
            <Route path="/disponibilidade" element={<Disponibilidade sessao={sessao} />} />
            <Route
              path="/quartos"
              element={<Cadastros key="quartos" tipo="quartos" sessao={sessao} />}
            />
            {equipe && (
              <>
                <Route
                  path="/clientes"
                  element={<Cadastros key="clientes" tipo="clientes" sessao={sessao} />}
                />
                <Route
                  path="/tipos"
                  element={<Cadastros key="tipos" tipo="tipos" sessao={sessao} />}
                />
              </>
            )}
            {admin && (
              <Route
                path="/usuarios"
                element={<Cadastros key="usuarios" tipo="usuarios" sessao={sessao} />}
              />
            )}
            <Route path="/conta" element={<Senha key="alterar" modo="alterar" sessao={sessao} />} />
            <Route path="/login" element={<Navigate to="/" replace />} />
            <Route
              path="*"
              element={
                <section className="empty">
                  <h1>Página indisponível</h1>
                  <p>Este endereço não existe ou não está disponível para seu perfil.</p>
                  <Link to="/">Voltar ao início</Link>
                </section>
              }
            />
          </Routes>
        </main>
        <footer className="app-footer">Hotel · Um lugar para chegar bem.</footer>
      </div>
    </div>
  )
}
