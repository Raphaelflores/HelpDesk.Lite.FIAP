import { defineStore } from 'pinia'
import { ref } from 'vue'
import dashboardService from 'src/services/dashboard'

/** Indicadores do dashboard. Uma leitura aqui tambem atualiza os gauges no backend. */
export const useDashboardStore = defineStore('dashboard', () => {
  const resumo = ref(null)
  const carregando = ref(false)

  async function carregar() {
    carregando.value = true
    try {
      const { data } = await dashboardService.resumo()
      resumo.value = data
      return data
    } finally {
      carregando.value = false
    }
  }

  return { resumo, carregando, carregar }
})
