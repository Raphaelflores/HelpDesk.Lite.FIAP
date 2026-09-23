import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import usuariosService from 'src/services/usuarios'
import { definirUsuarioAtual } from 'src/services/api'

/**
 * Sessao do login simulado (ADR-003).
 *
 * Guarda o usuario escolhido na tela de login e mantem o `src/services/api.js` em dia,
 * que e quem injeta o `X-User-Id` em cada requisicao.
 *
 * A sessao tambem e espelhada no `localStorage` para o F5 nao derrubar a demonstracao.
 * Isso NAO e seguranca: o backend revalida perfil e permissao a cada chamada.
 */
const CHAVE_SESSAO = 'helpdesk.usuario'

export const useAuthStore = defineStore('auth', () => {
  const usuario = ref(carregarSessao())
  const disponiveis = ref([])
  const carregando = ref(false)

  // Ao recarregar a pagina o modulo de api nasce sem usuario; devolve o id para ele.
  definirUsuarioAtual(usuario.value?.id ?? null)

  const autenticado = computed(() => usuario.value !== null)
  const perfil = computed(() => usuario.value?.perfil ?? null)
  const isAdmin = computed(() => perfil.value === 'ADMIN')
  const isAtendente = computed(() => perfil.value === 'ATENDENTE')
  const isSolicitante = computed(() => perfil.value === 'SOLICITANTE')

  /** Quem ve o dashboard e a fila completa de chamados. */
  const podeVerDashboard = computed(() => isAdmin.value || isAtendente.value)
  const podeGerenciarCadastros = computed(() => isAdmin.value)

  function carregarSessao() {
    try {
      const salvo = localStorage.getItem(CHAVE_SESSAO)
      return salvo ? JSON.parse(salvo) : null
    } catch {
      return null
    }
  }

  function guardarSessao(valor) {
    try {
      if (valor) {
        localStorage.setItem(CHAVE_SESSAO, JSON.stringify(valor))
      } else {
        localStorage.removeItem(CHAVE_SESSAO)
      }
    } catch {
      // Navegador sem localStorage: a sessao vive so em memoria. Nao e motivo para quebrar.
    }
  }

  async function carregarUsuariosDisponiveis() {
    carregando.value = true
    try {
      const { data } = await usuariosService.disponiveisParaLogin()
      disponiveis.value = data
      return data
    } finally {
      carregando.value = false
    }
  }

  async function entrar(usuarioId) {
    carregando.value = true
    try {
      const { data } = await usuariosService.login(usuarioId)
      usuario.value = data
      definirUsuarioAtual(data.id)
      guardarSessao(data)
      return data
    } finally {
      carregando.value = false
    }
  }

  function sair() {
    usuario.value = null
    definirUsuarioAtual(null)
    guardarSessao(null)
  }

  return {
    usuario,
    disponiveis,
    carregando,
    autenticado,
    perfil,
    isAdmin,
    isAtendente,
    isSolicitante,
    podeVerDashboard,
    podeGerenciarCadastros,
    carregarUsuariosDisponiveis,
    entrar,
    sair
  }
})
