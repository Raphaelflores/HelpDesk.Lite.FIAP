import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

// Axios mockado. O store roda de verdade, inclusive services e api.js.
const chamadas = []
const respostas = {}

vi.mock('axios', () => {
  const registrar = (metodo) => vi.fn((url, ...resto) => {
    const config = metodo === 'get' ? resto[0] : undefined
    const corpo = metodo === 'get' ? undefined : resto[0]
    chamadas.push({ metodo, url, params: config?.params, corpo })
    return Promise.resolve({ data: respostas[metodo] ?? {} })
  })

  const instancia = {
    defaults: { headers: {} },
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    get: registrar('get'),
    post: registrar('post'),
    patch: registrar('patch'),
    put: registrar('put'),
    delete: registrar('delete')
  }
  return { default: { create: () => instancia } }
})

const { useChamadosStore } = await import('src/stores/chamados')

function chamado(atributos = {}) {
  return {
    id: 1,
    titulo: 'Notebook nao liga',
    status: 'ABERTO',
    prioridade: 'ALTA',
    categoria: { id: 1, nome: 'TI', slaHoras: 4, ativa: true },
    solicitante: { id: 1, nome: 'Ana', perfil: 'SOLICITANTE' },
    atendente: null,
    comentarios: [],
    ...atributos
  }
}

function pagina(conteudo, extras = {}) {
  return {
    content: conteudo,
    totalElements: conteudo.length,
    totalPages: 1,
    number: 0,
    ...extras
  }
}

describe('chamadosStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    chamadas.length = 0
    Object.keys(respostas).forEach((chave) => delete respostas[chave])
  })

  describe('listagem', () => {
    it('carrega a lista e guarda os dados de paginacao', async () => {
      respostas.get = pagina([chamado(), chamado({ id: 2 })], { totalElements: 42, totalPages: 3 })
      const store = useChamadosStore()

      await store.carregarLista()

      expect(store.lista).toHaveLength(2)
      expect(store.paginacao.totalElements).toBe(42)
      expect(store.paginacao.totalPages).toBe(3)
      expect(store.carregando).toBe(false)
    })

    it('manda os filtros e a paginacao padrao como query string', async () => {
      respostas.get = pagina([])
      const store = useChamadosStore()

      await store.carregarLista()

      expect(chamadas.at(-1)).toMatchObject({
        metodo: 'get',
        url: '/chamados',
        params: { page: 0, size: 20, sort: 'criadoEm,desc' }
      })
    })

    it('aplica filtros e volta para a primeira pagina', async () => {
      respostas.get = pagina([])
      const store = useChamadosStore()
      store.paginacao.page = 4

      await store.aplicarFiltros({ status: 'ABERTO', prioridade: 'ALTA' })

      expect(store.filtros.status).toBe('ABERTO')
      expect(store.filtros.prioridade).toBe('ALTA')
      expect(chamadas.at(-1).params).toMatchObject({ status: 'ABERTO', prioridade: 'ALTA', page: 0 })
    })

    it('limpa os filtros e recarrega', async () => {
      respostas.get = pagina([])
      const store = useChamadosStore()
      await store.aplicarFiltros({ status: 'FECHADO', meus: true })

      await store.limparFiltros()

      expect(store.filtros).toEqual({ status: null, prioridade: null, categoriaId: null, meus: false })
      expect(chamadas.at(-1).params.status).toBeUndefined()
    })

    it('troca de pagina mantendo os filtros', async () => {
      respostas.get = pagina([], { number: 2 })
      const store = useChamadosStore()
      await store.aplicarFiltros({ categoriaId: 3 })

      await store.irParaPagina(2)

      expect(chamadas.at(-1).params).toMatchObject({ categoriaId: 3, page: 2 })
    })
  })

  describe('detalhe', () => {
    it('carrega o detalhe e separa os comentarios', async () => {
      const comentarios = [{ id: 1, texto: 'Ola', autor: { id: 2, nome: 'Bruno' } }]
      respostas.get = chamado({ comentarios })
      const store = useChamadosStore()

      await store.carregarDetalhe(1)

      expect(store.detalhe.id).toBe(1)
      expect(store.comentarios).toEqual(comentarios)
    })

    it('limpa o detalhe ao sair da tela', async () => {
      respostas.get = chamado({ comentarios: [{ id: 1 }] })
      const store = useChamadosStore()
      await store.carregarDetalhe(1)

      store.limparDetalhe()

      expect(store.detalhe).toBeNull()
      expect(store.comentarios).toEqual([])
    })
  })

  describe('acoes recarregam o que ficou desatualizado', () => {
    it('assumir atualiza o detalhe com a resposta do backend', async () => {
      respostas.get = chamado()
      const store = useChamadosStore()
      await store.carregarDetalhe(1)

      respostas.patch = chamado({
        status: 'EM_ATENDIMENTO',
        atendente: { id: 2, nome: 'Bruno', perfil: 'ATENDENTE' }
      })
      await store.assumir(1)

      expect(chamadas.at(-1)).toMatchObject({ metodo: 'patch', url: '/chamados/1/assumir' })
      expect(store.detalhe.status).toBe('EM_ATENDIMENTO')
      expect(store.detalhe.atendente.nome).toBe('Bruno')
    })

    it('alterar status atualiza tambem a linha correspondente da lista', async () => {
      respostas.get = pagina([chamado({ id: 1 }), chamado({ id: 2, titulo: 'Outro' })])
      const store = useChamadosStore()
      await store.carregarLista()

      respostas.patch = chamado({ id: 1, status: 'RESOLVIDO' })
      await store.alterarStatus(1, 'RESOLVIDO')

      expect(chamadas.at(-1)).toMatchObject({
        metodo: 'patch',
        url: '/chamados/1/status',
        corpo: { status: 'RESOLVIDO' }
      })
      expect(store.lista.find((c) => c.id === 1).status).toBe('RESOLVIDO')
      // A outra linha nao pode ser tocada.
      expect(store.lista.find((c) => c.id === 2).status).toBe('ABERTO')
    })

    it('comentar acrescenta o comentario na linha do tempo sem recarregar tudo', async () => {
      respostas.get = chamado({ comentarios: [{ id: 1, texto: 'Primeiro' }] })
      const store = useChamadosStore()
      await store.carregarDetalhe(1)

      respostas.post = { id: 2, texto: 'Segundo', autor: { id: 2, nome: 'Bruno' } }
      await store.comentar(1, 'Segundo')

      expect(chamadas.at(-1)).toMatchObject({
        metodo: 'post',
        url: '/chamados/1/comentarios',
        corpo: { texto: 'Segundo' }
      })
      expect(store.comentarios).toHaveLength(2)
      expect(store.comentarios.at(-1).texto).toBe('Segundo')
      expect(store.detalhe.comentarios).toHaveLength(2)
    })

    it('abrir chamado devolve o criado sem mexer na lista atual', async () => {
      respostas.get = pagina([chamado({ id: 1 })])
      const store = useChamadosStore()
      await store.carregarLista()

      respostas.post = chamado({ id: 99 })
      const criado = await store.abrir({ titulo: 'Novo', descricao: 'Descricao', categoriaId: 1, prioridade: 'ALTA' })

      expect(criado.id).toBe(99)
      expect(chamadas.at(-1)).toMatchObject({ metodo: 'post', url: '/chamados' })
      expect(store.lista).toHaveLength(1)
    })
  })
})
