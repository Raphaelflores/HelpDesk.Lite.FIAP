<template>
  <q-badge :color="cor" :label="rotulo" class="text-weight-medium q-px-sm q-py-xs" />
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true }
})

const CONFIGURACAO = {
  ABERTO: { cor: 'blue-7', rotulo: 'Aberto' },
  EM_ATENDIMENTO: { cor: 'orange-8', rotulo: 'Em atendimento' },
  RESOLVIDO: { cor: 'teal-7', rotulo: 'Resolvido' },
  FECHADO: { cor: 'grey-7', rotulo: 'Fechado' }
}

const cor = computed(() => CONFIGURACAO[props.status]?.cor ?? 'grey')
const rotulo = computed(() => CONFIGURACAO[props.status]?.rotulo ?? props.status)
</script>
