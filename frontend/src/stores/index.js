import { defineStore } from '#q-app/wrappers'
import { createPinia } from 'pinia'

/** Instancia do Pinia usada pelo Quasar CLI. */
export default defineStore(() => {
  return createPinia()
})
