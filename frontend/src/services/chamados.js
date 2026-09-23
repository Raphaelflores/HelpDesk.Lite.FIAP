import api from './api'

/** Endpoints de chamado. Toda URL de backend do dominio "chamado" mora aqui. */
export default {
  listar(filtros = {}, paginacao = {}) {
    return api.get('/chamados', {
      params: {
        status: filtros.status || undefined,
        prioridade: filtros.prioridade || undefined,
        categoriaId: filtros.categoriaId || undefined,
        meus: filtros.meus || undefined,
        page: paginacao.page ?? 0,
        size: paginacao.size ?? 20,
        sort: paginacao.sort || 'criadoEm,desc'
      }
    })
  },

  buscar(id) {
    return api.get(`/chamados/${id}`)
  },

  abrir(chamado) {
    return api.post('/chamados', chamado)
  },

  assumir(id) {
    return api.patch(`/chamados/${id}/assumir`)
  },

  alterarStatus(id, status) {
    return api.patch(`/chamados/${id}/status`, { status })
  },

  listarComentarios(id) {
    return api.get(`/chamados/${id}/comentarios`)
  },

  comentar(id, texto) {
    return api.post(`/chamados/${id}/comentarios`, { texto })
  }
}
