<template>
  <q-badge :color="cor" :label="rotulo" class="text-weight-medium q-px-sm q-py-xs">
    <q-icon :name="icone" size="14px" class="q-ml-xs" />
  </q-badge>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  prioridade: { type: String, required: true }
})

const CONFIGURACAO = {
  BAIXA: { cor: 'green-7', rotulo: 'Baixa', icone: 'south' },
  MEDIA: { cor: 'amber-8', rotulo: 'Media', icone: 'drag_handle' },
  ALTA: { cor: 'red-7', rotulo: 'Alta', icone: 'north' }
}

const cor = computed(() => CONFIGURACAO[props.prioridade]?.cor ?? 'grey')
const rotulo = computed(() => CONFIGURACAO[props.prioridade]?.rotulo ?? props.prioridade)
const icone = computed(() => CONFIGURACAO[props.prioridade]?.icone ?? 'help_outline')
</script>
