import api from './api'

export default {
  /** Publico: alimenta a tela de login simulado. */
  disponiveisParaLogin() {
    return api.get('/auth/usuarios-disponiveis')
  },

  login(usuarioId) {
    return api.post('/auth/login', { usuarioId })
  },

  listar() {
    return api.get('/usuarios')
  },

  criar(usuario) {
    return api.post('/usuarios', usuario)
  },

  atualizar(id, usuario) {
    return api.put(`/usuarios/${id}`, usuario)
  },

  desativar(id) {
    return api.delete(`/usuarios/${id}`)
  }
}
