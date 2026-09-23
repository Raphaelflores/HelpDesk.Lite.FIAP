import { defineStore } from 'pinia'
import { ref } from 'vue'
import chamadosService from 'src/services/chamados'

/**
 * Lista, filtros, paginacao e detalhe do chamado.
 *
 * As acoes (assumir, alterar status, comentar) recarregam o que ficou desatualizado --
 * e aqui que mora a regra de "recarregar depois da acao", nunca na page.
 */
export const useChamadosStore = defineStore('chamados', () => {
  const lista = ref([])
  const detalhe = ref(null)
  const comentarios = ref([])
  const carregando = ref(false)

  const filtros = ref({
    status: null,
    prioridade: null,
    categoriaId: null,
    meus: false
  })

  const paginacao = ref({
    page: 0,
    size: 20,
    sort: 'criadoEm,desc',
    totalElements: 0,
    totalPages: 0
  })

  async function carregarLista() {
    carregando.value = true
    try {
      const { data } = await chamadosService.listar(filtros.value, {
        page: paginacao.value.page,
        size: paginacao.value.size,
        sort: paginacao.value.sort
      })
      lista.value = data.content
      paginacao.value.totalElements = data.totalElements
      paginacao.value.totalPages = data.totalPages
      paginacao.value.page = data.number
      return data
    } finally {
      carregando.value = false
    }
  }

  /** Trocar filtro sempre volta para a primeira pagina. */
  async function aplicarFiltros(novosFiltros) {
    filtros.value = { ...filtros.value, ...novosFiltros }
    paginacao.value.page = 0
    return carregarLista()
  }

  async function limparFiltros() {
    filtros.value = { status: null, prioridade: null, categoriaId: null, meus: false }
    paginacao.value.page = 0
    return carregarLista()
  }

  async function irParaPagina(pagina) {
    paginacao.value.page = pagina
    return carregarLista()
  }

  async function carregarDetalhe(id) {
    carregando.value = true
    try {
      const { data } = await chamadosService.buscar(id)
      detalhe.value = data
      comentarios.value = data.comentarios ?? []
      return data
    } finally {
      carregando.value = false
    }
  }

  async function abrir(chamado) {
    const { data } = await chamadosService.abrir(chamado)
    return data
  }

  async function assumir(id) {
    const { data } = await chamadosService.assumir(id)
    aplicarAtualizacao(data)
    return data
  }

  async function alterarStatus(id, status) {
    const { data } = await chamadosService.alterarStatus(id, status)
    aplicarAtualizacao(data)
    return data
  }

  async function comentar(id, texto) {
    const { data } = await chamadosService.comentar(id, texto)
    comentarios.value = [...comentarios.value, data]
    if (detalhe.value?.id === id) {
      detalhe.value = { ...detalhe.value, comentarios: comentarios.value }
    }
    return data
  }

  /**
   * Depois de uma acao o backend devolve o chamado inteiro: aproveita a resposta para
   * atualizar detalhe e lista sem uma segunda ida ao servidor.
   */
  function aplicarAtualizacao(chamado) {
    if (detalhe.value?.id === chamado.id) {
      detalhe.value = chamado
      comentarios.value = chamado.comentarios ?? comentarios.value
    }
    lista.value = lista.value.map((item) => (item.id === chamado.id ? { ...item, ...chamado } : item))
  }

  function limparDetalhe() {
    detalhe.value = null
    comentarios.value = []
  }

  return {
    lista,
    detalhe,
    comentarios,
    carregando,
    filtros,
    paginacao,
    carregarLista,
    aplicarFiltros,
    limparFiltros,
    irParaPagina,
    carregarDetalhe,
    abrir,
    assumir,
    alterarStatus,
    comentar,
    limparDetalhe
  }
})
