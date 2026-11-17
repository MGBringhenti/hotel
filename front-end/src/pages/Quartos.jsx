import { useState } from 'react'
import { listarQuartos } from '../services/quartoService'
import { Link } from 'react-router-dom'

function Quartos({ sessao, onSessaoExpirada }) {
  const [pagina, setPagina] = useState(null)
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState('')

  async function carregarQuartos(numeroPagina = 0) {
    if (carregando) {
      return
    }

    if (Date.now() >= sessao.expiraEm) {
      onSessaoExpirada()
      return
    }

    setCarregando(true)
    setErro('')

    try {
      const dados = await listarQuartos(sessao.accessToken, numeroPagina)
      setPagina(dados)
    } catch (falha) {
      if (falha.response?.status === 401) {
        onSessaoExpirada()
      } else if (falha.response?.status === 403) {
        setErro('Você não tem permissão para consultar os quartos. ')
      } else {
        setErro('Não foi possível carregar os quartos. Tente novamente mais tarde.')
      }
    } finally {
      setCarregando(false)
    }
  }

  return (
    <section>
      <h2>Quartos</h2>

      <button type="button" onClick={() => carregarQuartos(0)} disabled={carregando}>
        {carregando ? 'Carregando...' : 'Carregar quartos'}
      </button>

      {erro && <p role="alert">{erro}</p>}

      {pagina && (
        <div>
          <p>Total de quartos cadastrados: {pagina.totalElements}</p>

          {pagina.content.length === 0 ? (
            <p>Nenhum quarto encontrado.</p>
          ) : (
            <ul>
              {pagina.content.map((quarto) => (
                <li key={quarto.id}>
                  <p>
                    Quarto {quarto.numero} — {quarto.tipo.nome}
                    {' — '}
                    {quarto.status}
                  </p>

                  {quarto.status === 'DISPONIVEL' && (
                    <Link to={`/disponibilidade?idQuarto=${quarto.id}`}>
                      Consultar datas disponíveis
                    </Link>
                  )}
                </li>
              ))}
            </ul>
          )}

          {pagina.totalPages > 0 && (
            <nav aria-label="Paginação dos quartos">
              <button
                type="button"
                disabled={carregando || pagina.first}
                onClick={() => carregarQuartos(pagina.number - 1)}
              >
                Anterior
              </button>

              <span>
                {' '}
                Página {pagina.number + 1} de {pagina.totalPages}{' '}
              </span>

              <button
                type="button"
                disabled={carregando || pagina.last}
                onClick={() => carregarQuartos(pagina.number + 1)}
              >
                Próxima
              </button>
            </nav>
          )}
        </div>
      )}
    </section>
  )
}

export default Quartos
