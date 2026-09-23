<template>
  <q-btn flat dense round :icon="icone" :color="cor" aria-label="Alternar tema" data-teste="seletor-tema">
    <q-tooltip>Tema: {{ rotuloAtual }}</q-tooltip>

    <q-menu auto-close anchor="bottom right" self="top right">
      <q-list style="min-width: 190px">
        <q-item-label header class="text-caption">Tema</q-item-label>

        <q-item
          v-for="opcao in OPCOES"
          :key="opcao.valor"
          v-ripple
          clickable
          :active="modo === opcao.valor"
          active-class="text-primary"
          :data-teste="`tema-${opcao.valor}`"
          @click="escolher(opcao.valor)"
        >
          <q-item-section avatar>
            <q-icon :name="opcao.icone" />
          </q-item-section>

          <q-item-section>
            <q-item-label>{{ opcao.rotulo }}</q-item-label>
            <q-item-label caption>{{ opcao.descricao }}</q-item-label>
          </q-item-section>

          <q-item-section v-if="modo === opcao.valor" side>
            <q-icon name="check" color="primary" />
          </q-item-section>
        </q-item>
      </q-list>
    </q-menu>
  </q-btn>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useQuasar } from 'quasar'
import { MODOS, aplicarTema, temaSalvo } from 'boot/tema'

/**
 * Escolha do tema: automatico, claro ou escuro.
 *
 * Fica no cabecalho do MainLayout e tambem na tela de login -- quem entra pela primeira
 * vez precisa conseguir trocar antes de ter sessao.
 */
const props = defineProps({
  /** Use `true` sobre o cabecalho colorido, onde o icone precisa ser branco. */
  claro: { type: Boolean, default: false }
})

const $q = useQuasar()
const modo = ref(temaSalvo())

const OPCOES = [
  {
    valor: MODOS.AUTO,
    rotulo: 'Automatico',
    descricao: 'Acompanha o sistema',
    icone: 'brightness_auto'
  },
  { valor: MODOS.CLARO, rotulo: 'Claro', descricao: 'Sempre claro', icone: 'light_mode' },
  { valor: MODOS.ESCURO, rotulo: 'Escuro', descricao: 'Sempre escuro', icone: 'dark_mode' }
]

/** O icone mostra o tema que esta valendo agora, nao o modo escolhido. */
const icone = computed(() => ($q.dark.isActive ? 'dark_mode' : 'light_mode'))

const cor = computed(() => (props.claro ? 'white' : undefined))

const rotuloAtual = computed(
  () => OPCOES.find((opcao) => opcao.valor === modo.value)?.rotulo ?? 'Automatico'
)

function escolher(valor) {
  modo.value = aplicarTema(valor)
}
</script>
