import { defineBoot } from '#q-app/wrappers'
import api from 'src/services/api'

/**
 * Deixa o cliente HTTP acessivel como `this.$api` na Options API.
 *
 * Codigo novo deve importar `src/services/*` direto -- esta ponte existe so para nao
 * espalhar imports em componentes legados.
 */
export default defineBoot(({ app }) => {
  app.config.globalProperties.$api = api
})

export { api }
