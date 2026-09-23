/**
 * Rotas da SPA.
 *
 * `meta.perfis` lista os perfis que podem abrir a rota. O guard em router/index.js usa
 * isso para redirecionar. Isso e UX: quem garante a regra e o backend, em toda chamada.
 */
const routes = [
  {
    path: '/login',
    component: () => import('layouts/LoginLayout.vue'),
    children: [
      { path: '', name: 'login', component: () => import('pages/LoginPage.vue'), meta: { publica: true } }
    ]
  },

  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', redirect: { name: 'chamados' } },
      {
        path: 'chamados',
        name: 'chamados',
        component: () => import('pages/ChamadosPage.vue'),
        meta: { titulo: 'Chamados' }
      },
      {
        path: 'chamados/novo',
        name: 'novo-chamado',
        component: () => import('pages/NovoChamadoPage.vue'),
        meta: { titulo: 'Novo chamado' }
      },
      {
        path: 'chamados/:id',
        name: 'chamado-detalhe',
        component: () => import('pages/ChamadoDetalhePage.vue'),
        meta: { titulo: 'Detalhe do chamado' }
      },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('pages/DashboardPage.vue'),
        meta: { titulo: 'Dashboard', perfis: ['ATENDENTE', 'ADMIN'] }
      },
      {
        path: 'categorias',
        name: 'categorias',
        component: () => import('pages/CategoriasPage.vue'),
        meta: { titulo: 'Categorias', perfis: ['ADMIN'] }
      },
      {
        path: 'usuarios',
        name: 'usuarios',
        component: () => import('pages/UsuariosPage.vue'),
        meta: { titulo: 'Usuarios', perfis: ['ADMIN'] }
      }
    ]
  },

  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErroNaoEncontradoPage.vue'),
    meta: { publica: true }
  }
]

export default routes
