import { defineRouter } from '#q-app/wrappers'
import { createMemoryHistory, createRouter, createWebHashHistory, createWebHistory } from 'vue-router'
import routes from './routes'
import { useAuthStore } from 'stores/auth'

export default defineRouter(function () {
  const createHistory = process.env.SERVER
    ? createMemoryHistory
    : (process.env.VUE_ROUTER_MODE === 'history' ? createWebHistory : createWebHashHistory)

  const Router = createRouter({
    scrollBehavior: () => ({ left: 0, top: 0 }),
    routes,
    history: createHistory(process.env.VUE_ROUTER_BASE)
  })

  /**
   * Guard de navegacao -- puramente UX:
   *  - sem sessao, manda para /login;
   *  - com sessao mas sem o perfil exigido pela rota, manda para /chamados.
   *
   * O backend continua validando tudo; aqui so evitamos mostrar uma tela que daria 403.
   */
  Router.beforeEach((para) => {
    const auth = useAuthStore()

    if (para.meta.publica) {
      return true
    }
    if (!auth.autenticado) {
      return { name: 'login', query: { destino: para.fullPath } }
    }
    if (Array.isArray(para.meta.perfis) && !para.meta.perfis.includes(auth.perfil)) {
      return { name: 'chamados' }
    }
    return true
  })

  return Router
})
