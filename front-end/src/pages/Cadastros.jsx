import { useState } from 'react'
import { Link } from 'react-router-dom'
import { catalogos } from '../lib/catalogos.js'
import { api, mensagemErro } from '../lib/api.js'
import { validarSenha, perfis } from '../lib/formatacao.js'
import { usePagina } from '../hooks/usePagina.js'
import { Aviso, Cabecalho, Campo, Modal, Paginacao, Situacao, Vazio } from '../components/UI.jsx'
import Seletor from '../components/Seletor.jsx'

function Formulario({ tipo, registro, sessao, onSalvo, onFechar }) {
  const config = catalogos[tipo]
  const [dados, setDados] = useState(() => ({
    ...Object.fromEntries(
      config.campos.map((c) => [c.nome, registro?.[c.nome] ?? (c.opcoes?.[0] || '')]),
    ),
    idTipo: registro?.tipo?.id ? String(registro.tipo.id) : '',
  }))
  const [ocupado, setOcupado] = useState(false)
  const [erro, setErro] = useState('')
  const [bloqueado, setBloqueado] = useState(false)
  function mudar(nome, valor) {
    setDados((atual) => ({ ...atual, [nome]: valor }))
  }
  async function salvar(e) {
    e.preventDefault()
    if (ocupado || bloqueado) return
    setErro('')
    if (tipo === 'quartos' && !dados.idTipo) {
      setErro('Selecione um tipo de quarto.')
      return
    }
    if (tipo === 'usuarios' && !registro && validarSenha(dados.senha)) {
      setErro(validarSenha(dados.senha))
      return
    }
    const corpo = {}
    config.campos
      .filter((c) => !registro || !c.criacao)
      .forEach((c) => {
        corpo[c.nome] =
          c.type === 'number'
            ? Number(dados[c.nome])
            : c.type === 'password'
              ? dados[c.nome]
              : dados[c.nome].trim()
      })
    if (tipo === 'clientes') corpo.cpf = corpo.cpf.replace(/\D/g, '')
    if (tipo === 'quartos') corpo.idTipo = Number(dados.idTipo)
    if (registro) corpo.versao = registro.versao
    setOcupado(true)
    try {
      await api(config.servico, config.caminho + (registro ? `/${registro.id}` : ''), {
        sessao,
        method: registro ? 'PUT' : 'POST',
        data: corpo,
      })
      onSalvo()
    } catch (falha) {
      setErro(mensagemErro(falha, true))
      if (falha.response?.status === 409 || !falha.response || falha.response?.status >= 500)
        setBloqueado(true)
    } finally {
      setOcupado(false)
    }
  }
  return (
    <Modal
      titulo={`${registro ? 'Editar' : 'Cadastrar'} ${config.singular}`}
      ocupado={ocupado}
      onFechar={onFechar}
    >
      <form onSubmit={salvar}>
        <fieldset disabled={ocupado || bloqueado} className="form-grid">
          {config.campos
            .filter((c) => !registro || !c.criacao)
            .map((c) => (
              <Campo key={c.nome} label={c.label}>
                {c.opcoes ? (
                  <select
                    required
                    value={dados[c.nome]}
                    onChange={(e) => mudar(c.nome, e.target.value)}
                  >
                    {c.opcoes.map((o) => (
                      <option key={o} value={o}>
                        {perfis[o] || o}
                      </option>
                    ))}
                  </select>
                ) : (
                  <input
                    type={c.type || 'text'}
                    min={c.min}
                    max={c.max}
                    step={c.step}
                    minLength={c.minLength}
                    maxLength={c.maxLength}
                    required
                    value={dados[c.nome]}
                    autoComplete={c.type === 'password' ? 'new-password' : undefined}
                    onChange={(e) => mudar(c.nome, e.target.value)}
                  />
                )}
              </Campo>
            ))}
          {tipo === 'quartos' && (
            <div className="full">
              <Seletor
                tipo="tipos"
                sessao={sessao}
                valor={dados.idTipo}
                onChange={(v) => mudar('idTipo', v)}
                disabled={ocupado || bloqueado}
              />
            </div>
          )}
          {tipo === 'usuarios' && registro && (
            <p className="full muted">
              Perfil: {perfis[registro.funcao]}. O perfil de uma conta existente não pode ser
              modificado.
            </p>
          )}
          {tipo === 'usuarios' && !registro && (
            <p className="full muted">
              Vincule a conta de hóspede a um cliente na tela Clientes antes do primeiro login.
            </p>
          )}
          <div className="full actions">
            <button type="submit">{ocupado ? 'Salvando...' : 'Salvar'}</button>
          </div>
        </fieldset>
      </form>
      <Aviso erro={erro} />
      {bloqueado && <p>Feche esta janela e atualize a lista antes de tentar novamente.</p>}
    </Modal>
  )
}
function Vinculo({ cliente, sessao, onFechar, onSalvo }) {
  const [usuario, setUsuario] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const [erro, setErro] = useState('')
  async function salvar(e) {
    e.preventDefault()
    if (ocupado) return
    if (!usuario) {
      setErro('Selecione uma conta de hóspede.')
      return
    }
    if (
      !window.confirm(
        `Vincular a conta selecionada ao cadastro de ${cliente.nome}? Confira sua escolha antes de continuar.`,
      )
    )
      return
    setOcupado(true)
    try {
      await api('clientes', `/clientes/${cliente.id}/usuario`, {
        sessao,
        method: 'PUT',
        data: { usuarioId: Number(usuario) },
      })
      onSalvo()
    } catch (falha) {
      setErro(mensagemErro(falha, true))
    } finally {
      setOcupado(false)
    }
  }
  return (
    <Modal titulo={`Vincular usuário a ${cliente.nome}`} ocupado={ocupado} onFechar={onFechar}>
      <p>
        Selecione uma conta ativa com perfil de hóspede. O vínculo será validado antes de salvar.
      </p>
      <form onSubmit={salvar}>
        <Seletor
          tipo="usuarios"
          sessao={sessao}
          valor={usuario}
          onChange={setUsuario}
          disabled={ocupado}
        />
        <button disabled={ocupado || !usuario}>Vincular conta</button>
      </form>
      <Aviso erro={erro} />
    </Modal>
  )
}
export default function Cadastros({ tipo, sessao }) {
  const config = catalogos[tipo]
  const lista = usePagina(config.servico, config.caminho, sessao)
  const [editor, setEditor] = useState(null)
  const [vinculo, setVinculo] = useState(null)
  const [ocupado, setOcupado] = useState(false)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const admin = sessao.funcao === 'ADMIN'
  const equipe = admin || sessao.funcao === 'RECEPCIONISTA'
  const podeEditar = ['usuarios', 'tipos'].includes(tipo) ? admin : equipe
  function salvo() {
    setEditor(null)
    setVinculo(null)
    setErro('')
    setSucesso('Cadastro salvo com sucesso.')
    lista.recarregar()
  }
  async function executar(item, excluir = false) {
    if (ocupado) return
    const ativo = tipo === 'quartos' ? item.status === 'DISPONIVEL' : item.ativo
    if (
      !window.confirm(
        excluir
          ? 'Excluir este tipo de quarto? A exclusão só será aceita se não houver quartos vinculados.'
          : `Deseja ${ativo ? 'inativar' : 'ativar'} este cadastro?`,
      )
    )
      return
    setOcupado(true)
    setErro('')
    setSucesso('')
    try {
      const sufixo = excluir ? '' : tipo === 'quartos' ? '/status' : '/situacao'
      const data =
        tipo === 'quartos'
          ? { status: ativo ? 'INATIVO' : 'DISPONIVEL', versao: item.versao }
          : { ativo: !ativo, versao: item.versao }
      await api(config.servico, `${config.caminho}/${item.id}${sufixo}`, {
        sessao,
        method: excluir ? 'DELETE' : 'PATCH',
        data: excluir ? undefined : data,
      })
      setSucesso(excluir ? 'Tipo de quarto excluído.' : 'Situação atualizada.')
    } catch (falha) {
      setErro(mensagemErro(falha, true))
    } finally {
      setOcupado(false)
      lista.recarregar()
    }
  }
  return (
    <>
      <Cabecalho titulo={config.titulo} descricao={config.descricao}>
        <button
          className="secondary"
          disabled={ocupado || lista.carregando}
          onClick={lista.recarregar}
        >
          Atualizar
        </button>
        {podeEditar && <button onClick={() => setEditor({})}>Cadastrar {config.singular}</button>}
      </Cabecalho>
      <Aviso erro={erro || lista.erro} sucesso={sucesso} />
      {lista.carregando ? (
        <p role="status" className="loading">
          Carregando cadastros...
        </p>
      ) : !lista.pagina?.content.length ? (
        <Vazio>
          Não há registros para exibir. {podeEditar && 'Use o botão de cadastro para começar.'}
        </Vazio>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                {config.colunas.map(([nome]) => (
                  <th key={nome}>{nome}</th>
                ))}
                {tipo !== 'tipos' && <th>Situação</th>}
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {lista.pagina.content.map((item) => (
                <tr key={item.id}>
                  {config.colunas.map(([nome, obter]) => (
                    <td key={nome}>{obter(item)}</td>
                  ))}
                  {tipo !== 'tipos' && (
                    <td>
                      <Situacao valor={tipo === 'quartos' ? item.status : item.ativo} />
                    </td>
                  )}
                  <td>
                    <div className="actions">
                      {tipo === 'quartos' && item.status === 'DISPONIVEL' && (
                        <Link className="button small" to={`/disponibilidade?idQuarto=${item.id}`}>
                          Ver datas
                        </Link>
                      )}
                      {podeEditar && (
                        <button
                          className="secondary small"
                          disabled={ocupado}
                          onClick={() => setEditor(item)}
                        >
                          Editar
                        </button>
                      )}
                      {admin && tipo !== 'tipos' && (
                        <button
                          className="secondary small"
                          disabled={ocupado}
                          onClick={() => executar(item)}
                        >
                          {(tipo === 'quartos' ? item.status === 'DISPONIVEL' : item.ativo)
                            ? 'Inativar'
                            : 'Ativar'}
                        </button>
                      )}
                      {admin && tipo === 'clientes' && (
                        <button
                          className="secondary small"
                          disabled={ocupado}
                          onClick={() => setVinculo(item)}
                        >
                          Vincular usuário
                        </button>
                      )}
                      {admin && tipo === 'tipos' && (
                        <button
                          className="danger small"
                          disabled={ocupado}
                          onClick={() => executar(item, true)}
                        >
                          Excluir
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Paginacao
        pagina={lista.pagina}
        onChange={lista.irPara}
        disabled={ocupado || lista.carregando}
      />
      {editor && (
        <Formulario
          tipo={tipo}
          registro={editor.id ? editor : null}
          sessao={sessao}
          onFechar={() => setEditor(null)}
          onSalvo={salvo}
        />
      )}
      {vinculo && (
        <Vinculo
          cliente={vinculo}
          sessao={sessao}
          onFechar={() => setVinculo(null)}
          onSalvo={salvo}
        />
      )}
    </>
  )
}
