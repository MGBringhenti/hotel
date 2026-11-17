import { useEffect, useState } from 'react'
import { api, mensagemErro } from '../lib/api.js'
export function usePagina(servico, caminho, sessao) {
  const [numero, setNumero] = useState(0)
  const [revisao, setRevisao] = useState(0)
  const [estado, setEstado] = useState({ pagina: null, carregando: true, erro: '' })
  useEffect(() => {
    const controller = new AbortController()
    let ativo = true
    api(servico, caminho, {
      sessao,
      signal: controller.signal,
      params: { pagina: numero, tamanho: 12 },
    })
      .then((pagina) => {
        if (ativo) setEstado({ pagina, carregando: false, erro: '' })
      })
      .catch((erro) => {
        if (ativo) setEstado({ pagina: null, carregando: false, erro: mensagemErro(erro) })
      })
    return () => {
      ativo = false
      controller.abort()
    }
  }, [servico, caminho, sessao, numero, revisao])
  function irPara(valor) {
    setEstado({ pagina: null, carregando: true, erro: '' })
    setNumero(valor)
    setRevisao((v) => v + 1)
  }
  return { ...estado, numero, irPara, recarregar: () => irPara(numero) }
}
