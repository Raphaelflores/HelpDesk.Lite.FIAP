<template>
  <q-page class="flex flex-center" :class="$q.dark.isActive ? '' : 'bg-grey-2'">
    <!-- A tela de login nao tem cabecalho, entao o seletor de tema flutua no canto. -->
    <div class="absolute-top-right q-pa-md">
      <SeletorTema />
    </div>

    <q-card flat bordered class="q-pa-lg" style="width: 440px; max-width: 92vw">
      <div class="text-center q-mb-lg">
        <q-icon name="support_agent" size="52px" color="primary" />
        <div class="text-h5 q-mt-sm">HelpDesk Lite</div>
        <div class="text-caption text-grey-7">
          Login simulado: escolha com qual usuario entrar.
        </div>
      </div>

      <q-inner-loading :showing="auth.carregando">
        <q-spinner-dots size="40px" color="primary" />
      </q-inner-loading>

      <q-list v-if="!auth.carregando" bordered separator class="rounded-borders">
        <q-item
          v-for="usuario in auth.disponiveis"
          :key="usuario.id"
          v-ripple
          clickable
          :data-teste="`usuario-${usuario.id}`"
          @click="entrar(usuario)"
        >
          <q-item-section avatar>
            <q-avatar :color="corDoPerfil(usuario.perfil)" text-color="white">
              {{ usuario.nome.charAt(0) }}
            </q-avatar>
          </q-item-section>

          <q-item-section>
            <q-item-label>{{ usuario.nome }}</q-item-label>
            <q-item-label caption>{{ usuario.email }}</q-item-label>
          </q-item-section>

          <q-item-section side>
            <q-badge :color="corDoPerfil(usuario.perfil)" :label="ROTULOS[usuario.perfil]" />
          </q-item-section>
        </q-item>
      </q-list>

      <q-banner
        v-if="!auth.carregando && auth.disponiveis.length === 0"
        dense
        class="q-mt-md"
        :class="$q.dark.isActive ? 'bg-orange-10' : 'bg-orange-1'"
      >
        <template #avatar><q-icon name="warning" color="orange-8" /></template>
        Nenhum usuario retornado. A API esta rodando em <code>localhost:8080</code>?
      </q-banner>

      <div class="text-caption text-grey-6 q-mt-lg text-center">
        Nao ha senha nem token. O id do usuario escolhido vai no header
        <code>X-User-Id</code> de cada requisicao.
      </div>
    </q-card>
  </q-page>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import SeletorTema from 'components/SeletorTema.vue'
import { useAuthStore } from 'stores/auth'

const $q = useQuasar()
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const ROTULOS = {
  SOLICITANTE: 'Solicitante',
  ATENDENTE: 'Atendente',
  ADMIN: 'Admin'
}

const CORES = {
  SOLICITANTE: 'blue-7',
  ATENDENTE: 'teal-7',
  ADMIN: 'purple-6'
}

function corDoPerfil(perfil) {
  return CORES[perfil] ?? 'grey-7'
}

onMounted(() => {
  // Erro de rede ja vira q-notify no interceptor; aqui so evitamos derrubar a tela.
  auth.carregarUsuariosDisponiveis().catch(() => {})
})

async function entrar(usuario) {
  await auth.entrar(usuario.id)
  router.push(route.query.destino || { name: 'chamados' })
}
</script>
