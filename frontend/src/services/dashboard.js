import api from './api'

export default {
  resumo() {
    return api.get('/dashboard/resumo')
  }
}
