<template>
  <q-layout view="hHh lpR fFf">
    <q-header elevated class="bg-primary text-white">
      <q-toolbar>
        <q-btn flat dense round icon="menu" aria-label="Menu" @click="menuAberto = !menuAberto" />

        <q-toolbar-title class="row items-center q-gutter-sm">
          <q-icon name="support_agent" size="24px" />
          <span>HelpDesk Lite</span>
        </q-toolbar-title>

        <div class="row items-center q-gutter-sm">
          <SeletorTema claro />

          <template v-if="auth.usuario">
            <div class="column items-end gt-xs">
              <div class="text-body2">{{ auth.usuario.nome }}</div>
              <div class="text-caption text-blue-2">{{ rotuloPerfil }}</div>
            </div>
            <q-btn flat dense no-caps icon="logout" label="Sair" data-teste="sair" @click="sair" />
          </template>
        </div>
      </q-toolbar>
    </q-header>

    <q-drawer v-model="menuAberto" show-if-above bordered :width="240">
      <q-list padding>
        <q-item-label header class="text-grey-7">Navegacao</q-item-label>

        <q-item
          v-for="item in itensVisiveis"
          :key="item.rota"
          v-ripple
          clickable
          :to="{ name: item.rota }"
          exact
        >
          <q-item-section avatar><q-icon :name="item.icone" /></q-item-section>
          <q-item-section>{{ item.rotulo }}</q-item-section>
        </q-item>
      </q-list>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import SeletorTema from 'components/SeletorTema.vue'
import { useAuthStore } from 'stores/auth'

const router = useRouter()
const auth = useAuthStore()
const menuAberto = ref(false)

const ROTULOS_PERFIL = {
  SOLICITANTE: 'Solicitante',
  ATENDENTE: 'Atendente',
  ADMIN: 'Administrador'
}

const rotuloPerfil = computed(() => ROTULOS_PERFIL[auth.perfil] ?? auth.perfil)

/**
 * O menu esconde o que o perfil nao pode acessar. Isso e UX -- o backend recusa de
 * qualquer jeito, e o router guard ainda barra quem digitar a URL na mao.
 */
const itens = computed(() => [
  { rota: 'chamados', rotulo: 'Chamados', icone: 'list_alt', visivel: true },
  { rota: 'novo-chamado', rotulo: 'Novo chamado', icone: 'add_circle_outline', visivel: true },
  { rota: 'dashboard', rotulo: 'Dashboard', icone: 'insights', visivel: auth.podeVerDashboard },
  { rota: 'categorias', rotulo: 'Categorias', icone: 'category', visivel: auth.podeGerenciarCadastros },
  { rota: 'usuarios', rotulo: 'Usuarios', icone: 'group', visivel: auth.podeGerenciarCadastros }
])

const itensVisiveis = computed(() => itens.value.filter((item) => item.visivel))

function sair() {
  auth.sair()
  router.push({ name: 'login' })
}
</script>
