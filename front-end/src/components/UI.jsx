import { cloneElement, useEffect, useId, useRef } from 'react'
export function Aviso({ erro, sucesso }) {
  return (
    <>
      {erro && (
        <div className="aviso erro" role="alert">
          {erro}
        </div>
      )}
      {sucesso && (
        <div className="aviso sucesso" role="status">
          {sucesso}
        </div>
      )}
    </>
  )
}
export function Cabecalho({ titulo, descricao, children }) {
  return (
    <header className="page-header">
      <div>
        <h1>{titulo}</h1>
        <p>{descricao}</p>
      </div>
      <div className="actions">{children}</div>
    </header>
  )
}
export function Paginacao({ pagina, onChange, disabled }) {
  if (!pagina || pagina.totalPages < 1) return null
  return (
    <nav className="pagination" aria-label="Paginação">
      <span>
        {pagina.totalElements} registros · Página {pagina.number + 1} de {pagina.totalPages}
      </span>
      <div className="actions">
        <button
          type="button"
          className="secondary"
          disabled={disabled || pagina.first}
          onClick={() => onChange(pagina.number - 1)}
        >
          Anterior
        </button>
        <button
          type="button"
          className="secondary"
          disabled={disabled || pagina.last}
          onClick={() => onChange(pagina.number + 1)}
        >
          Próxima
        </button>
      </div>
    </nav>
  )
}
export function Situacao({ valor }) {
  const nome = typeof valor === 'boolean' ? (valor ? 'Ativo' : 'Inativo') : valor
  return <span className={`badge ${String(nome).toLowerCase()}`}>{nome?.replaceAll('_', ' ')}</span>
}
export function Vazio({ children = 'Nenhum registro encontrado.' }) {
  return (
    <div className="empty">
      <span aria-hidden="true">◇</span>
      <p>{children}</p>
    </div>
  )
}
export function Modal({ titulo, onFechar, ocupado, children }) {
  const ref = useRef(null)
  useEffect(() => {
    const dialog = ref.current
    dialog.showModal()
    return () => dialog.close()
  }, [])
  return (
    <dialog
      ref={ref}
      aria-label={titulo}
      onCancel={(e) => {
        e.preventDefault()
        if (!ocupado) onFechar()
      }}
    >
      <header className="modal-header">
        <h2>{titulo}</h2>
        <button
          type="button"
          className="secondary"
          disabled={ocupado}
          onClick={onFechar}
          aria-label="Fechar janela"
        >
          Fechar
        </button>
      </header>
      {children}
    </dialog>
  )
}
export function Campo({ label, children, ...props }) {
  const id = useId()
  const labelId = id + '-label'
  return (
    <label className="field" htmlFor={id}>
      <span id={labelId}>{label}</span>
      {children ? (
        cloneElement(children, { id, 'aria-labelledby': labelId })
      ) : (
        <input {...props} id={id} aria-labelledby={labelId} />
      )}
    </label>
  )
}
