import axios from 'axios'
import { Notify } from 'quasar'

/**
 * Cliente HTTP da aplicacao.
 *
 * Este e o UNICO arquivo que conhece:
 *  - a URL do backend;
 *  - o header `X-User-Id` da autenticacao simulada;
 *  - o header `X-Request-Id` que correlaciona a requisicao com o log do servidor;
 *  - a traducao do JSON de erro do backend em `q-notify`.
 *
 * Nenhum outro arquivo pode montar URL de backend nem tratar erro de HTTP.
 */

const BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080/api'

export const HEADER_USUARIO = 'X-User-Id'
export const HEADER_REQUEST_ID = 'X-Request-Id'

/**
 * Id do usuario logado. O authStore mantem isso em dia chamando `definirUsuarioAtual`.
 *
 * Fica num modulo em vez de vir do Pinia dentro do interceptor para nao criar dependencia
 * circular entre store e service -- e para o teste conseguir controlar o valor sem montar
 * uma aplicacao inteira.
 */
let usuarioAtualId = null

export function definirUsuarioAtual(id) {
  usuarioAtualId = id ?? null
}

export function usuarioAtual() {
  return usuarioAtualId
}

/** Id de correlacao por requisicao. O backend devolve o mesmo valor no header da resposta. */
function gerarRequestId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `req-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`
}

export const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use((config) => {
  config.headers[HEADER_REQUEST_ID] = gerarRequestId()
  if (usuarioAtualId !== null && usuarioAtualId !== undefined) {
    config.headers[HEADER_USUARIO] = String(usuarioAtualId)
  }
  return config
})

/** Monta a mensagem do notify: o texto que o backend mandou, mais a lista de campos no 400. */
function mensagemDoErro(erro) {
  const corpo = erro.response?.data

  if (!corpo) {
    return 'Nao foi possivel falar com o servidor. A API esta rodando em ' + BASE_URL + '?'
  }
  if (Array.isArray(corpo.campos) && corpo.campos.length > 0) {
    const campos = corpo.campos.map((c) => `${c.campo}: ${c.mensagem}`).join('; ')
    return `${corpo.mensagem} (${campos})`
  }
  return corpo.mensagem || 'Erro inesperado ao chamar a API'
}

api.interceptors.response.use(
  (resposta) => resposta,
  (erro) => {
    const status = erro.response?.status
    // O backend devolve o mesmo id que enviamos; o fallback cobre erro de rede,
    // em que nao ha resposta nenhuma.
    const requestId =
      erro.response?.headers?.[HEADER_REQUEST_ID.toLowerCase()] ||
      erro.config?.headers?.[HEADER_REQUEST_ID] ||
      'sem id'

    Notify.create({
      type: status === 401 || status === 403 ? 'warning' : 'negative',
      message: mensagemDoErro(erro),
      caption: `${status ? 'HTTP ' + status + ' · ' : ''}request id: ${requestId}`,
      icon: 'error_outline'
    })

    return Promise.reject(erro)
  }
)

export default api
