import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api, mensagemErro } from '../lib/api.js'
import { validarSenha } from '../lib/formatacao.js'
import { Aviso, Campo, Cabecalho } from '../components/UI.jsx'
export default function Senha({ modo, sessao }) {
  const [email, setEmail] = useState('')
  const [token, setToken] = useState('')
  const [atual, setAtual] = useState('')
  const [nova, setNova] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const recuperar = modo === 'recuperar'
  const alterar = modo === 'alterar'
  const titulo = recuperar ? 'Recuperar acesso' : alterar ? 'Minha conta' : 'Definir nova senha'
  async function enviar(e) {
    e.preventDefault()
    if (ocupado) return
    setErro('')
    setSucesso('')
    if (!recuperar) {
      const problema = validarSenha(nova)
      if (problema) {
        setErro(problema)
        return
      }
      if (nova !== confirmacao) {
        setErro('A confirmação deve ser igual à nova senha.')
        return
      }
      if (alterar && atual === nova) {
        setErro('A nova senha deve ser diferente da atual.')
        return
      }
    }
    setOcupado(true)
    try {
      if (recuperar) {
        await api('clientes', '/auth/recuperacao-senha', {
          method: 'POST',
          data: { emailRecuperacao: email.trim() },
        })
        setSucesso(
          'Se houver uma conta ativa com esse e-mail, você receberá as instruções de recuperação.',
        )
      } else {
        await api('clientes', alterar ? '/auth/senha' : '/auth/recuperacao-senha/redefinir', {
          sessao: alterar ? sessao : undefined,
          method: alterar ? 'PATCH' : 'POST',
          data: alterar
            ? { senhaAtual: atual, senhaNova: nova }
            : { token: token.trim(), senhaNova: nova },
        })
        setSucesso(
          alterar
            ? 'Senha alterada com sucesso.'
            : 'Senha redefinida. Você já pode entrar com a nova senha.',
        )
        setAtual('')
        setNova('')
        setConfirmacao('')
        setToken('')
      }
    } catch (falha) {
      setErro(mensagemErro(falha, true))
    } finally {
      setOcupado(false)
    }
  }
  return (
    <section className={alterar ? 'account-page' : 'public-panel'}>
      <Cabecalho
        titulo={titulo}
        descricao={
          recuperar
            ? 'Informe o e-mail de recuperação cadastrado na sua conta.'
            : alterar
              ? 'Atualize sua senha de acesso.'
              : 'Use o código recebido por e-mail. Ele vale por 30 minutos e só pode ser usado uma vez.'
        }
      />
      <form onSubmit={enviar} className="account-form">
        <fieldset className="stack" disabled={ocupado}>
          {recuperar ? (
            <Campo
              label="E-mail de recuperação"
              type="email"
              autoComplete="email"
              maxLength={254}
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          ) : (
            <>
              {alterar ? (
                <Campo
                  label="Senha atual"
                  type="password"
                  autoComplete="current-password"
                  required
                  maxLength={50}
                  value={atual}
                  onChange={(e) => setAtual(e.target.value)}
                />
              ) : (
                <Campo
                  label="Código recebido por e-mail"
                  autoComplete="one-time-code"
                  maxLength={200}
                  required
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                />
              )}
              <Campo
                label="Nova senha"
                type="password"
                autoComplete="new-password"
                minLength={8}
                maxLength={50}
                required
                value={nova}
                onChange={(e) => setNova(e.target.value)}
              />
              <Campo
                label="Confirmar nova senha"
                type="password"
                autoComplete="new-password"
                minLength={8}
                maxLength={50}
                required
                value={confirmacao}
                onChange={(e) => setConfirmacao(e.target.value)}
              />
            </>
          )}
          <button>
            {ocupado ? 'Enviando...' : recuperar ? 'Enviar instruções' : 'Salvar nova senha'}
          </button>
        </fieldset>
      </form>
      <Aviso erro={erro} sucesso={sucesso} />
      {!alterar && (
        <div className="actions public-links">
          <Link to="/login">Voltar ao login</Link>
          <Link to={recuperar ? '/redefinir-senha' : '/recuperar-senha'}>
            {recuperar ? 'Já tenho o código' : 'Solicitar outro código'}
          </Link>
        </div>
      )}
    </section>
  )
}
