import api from './api'

export default {
  listar(incluirInativas = false) {
    return api.get('/categorias', { params: { incluirInativas: incluirInativas || undefined } })
  },

  criar(categoria) {
    return api.post('/categorias', categoria)
  },

  atualizar(id, categoria) {
    return api.put(`/categorias/${id}`, categoria)
  },

  desativar(id) {
    return api.delete(`/categorias/${id}`)
  }
}
