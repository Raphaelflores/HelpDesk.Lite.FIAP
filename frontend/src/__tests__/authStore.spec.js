import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

// Axios mockado: o store exercita services + api.js de verdade, so a ida a rede e falsa.
const requisicoes = []

vi.mock('axios', () => {
  const instancia = {
    defaults: { headers: {} },
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() }
    },
    get: vi.fn((url, config) => {
      requisicoes.push({ metodo: 'get', url, config })
      return Promise.resolve({ data: respostas.get ?? [] })
    }),
    post: vi.fn((url, corpo) => {
      requisicoes.push({ metodo: 'post', url, corpo })
      return Promise.resolve({ data: respostas.post ?? {} })
    })
  }
  return { default: { create: () => instancia } }
})

const respostas = {}

const ANA = { id: 1, nome: 'Ana Solicitante', email: 'ana@empresa.com', perfil: 'SOLICITANTE', ativo: true }
const BRUNO = { id: 2, nome: 'Bruno Atendente', email: 'bruno@empresa.com', perfil: 'ATENDENTE', ativo: true }
const CARLA = { id: 3, nome: 'Carla Admin', email: 'carla@empresa.com', perfil: 'ADMIN', ativo: true }

const { useAuthStore } = await import('src/stores/auth')
const { usuarioAtual } = await import('src/services/api')

describe('authStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    requisicoes.length = 0
    delete respostas.get
    delete respostas.post
  })

  it('comeca sem sessao', () => {
    const auth = useAuthStore()

    expect(auth.autenticado).toBe(false)
    expect(auth.perfil).toBeNull()
    expect(auth.usuario).toBeNull()
  })

  it('carrega os usuarios disponiveis para a tela de login', async () => {
    respostas.get = [ANA, BRUNO, CARLA]
    const auth = useAuthStore()

    await auth.carregarUsuariosDisponiveis()

    expect(auth.disponiveis).toHaveLength(3)
    expect(requisicoes.at(-1).url).toBe('/auth/usuarios-disponiveis')
    expect(auth.carregando).toBe(false)
  })

  it('guarda o usuario ao entrar e informa o id ao cliente HTTP', async () => {
    respostas.post = BRUNO
    const auth = useAuthStore()

    await auth.entrar(2)

    expect(auth.autenticado).toBe(true)
    expect(auth.usuario).toEqual(BRUNO)
    expect(requisicoes.at(-1)).toMatchObject({ url: '/auth/login', corpo: { usuarioId: 2 } })
    // E o api.js que injeta o X-User-Id; o store precisa manter esse valor em dia.
    expect(usuarioAtual()).toBe(2)
  })

  describe('helpers de perfil', () => {
    it('reconhece o solicitante e nega dashboard e cadastros', async () => {
      respostas.post = ANA
      const auth = useAuthStore()

      await auth.entrar(1)

      expect(auth.isSolicitante).toBe(true)
      expect(auth.isAtendente).toBe(false)
      expect(auth.isAdmin).toBe(false)
      expect(auth.podeVerDashboard).toBe(false)
      expect(auth.podeGerenciarCadastros).toBe(false)
    })

    it('reconhece o atendente: ve dashboard, nao gerencia cadastros', async () => {
      respostas.post = BRUNO
      const auth = useAuthStore()

      await auth.entrar(2)

      expect(auth.isAtendente).toBe(true)
      expect(auth.podeVerDashboard).toBe(true)
      expect(auth.podeGerenciarCadastros).toBe(false)
    })

    it('reconhece o admin: ve dashboard e gerencia cadastros', async () => {
      respostas.post = CARLA
      const auth = useAuthStore()

      await auth.entrar(3)

      expect(auth.isAdmin).toBe(true)
      expect(auth.podeVerDashboard).toBe(true)
      expect(auth.podeGerenciarCadastros).toBe(true)
    })
  })

  it('limpa a sessao e o id do cliente HTTP ao sair', async () => {
    respostas.post = CARLA
    const auth = useAuthStore()
    await auth.entrar(3)

    auth.sair()

    expect(auth.autenticado).toBe(false)
    expect(auth.usuario).toBeNull()
    expect(usuarioAtual()).toBeNull()
    expect(localStorage.getItem('helpdesk.usuario')).toBeNull()
  })

  it('recupera a sessao do localStorage depois de um F5', async () => {
    localStorage.setItem('helpdesk.usuario', JSON.stringify(BRUNO))

    const auth = useAuthStore()

    expect(auth.autenticado).toBe(true)
    expect(auth.perfil).toBe('ATENDENTE')
    expect(usuarioAtual()).toBe(2)
  })

  it('ignora sessao corrompida no localStorage em vez de quebrar', () => {
    localStorage.setItem('helpdesk.usuario', '{isso nao e json')

    const auth = useAuthStore()

    expect(auth.autenticado).toBe(false)
  })
})
