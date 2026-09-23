<template>
  <div>
    <q-timeline v-if="comentarios.length > 0" color="primary" layout="dense">
      <q-timeline-entry
        v-for="comentario in comentarios"
        :key="comentario.id"
        :subtitle="`${comentario.autor.nome} · ${formatar(comentario.criadoEm)}`"
        :icon="iconePorPerfil(comentario.autor.perfil)"
        :color="corPorPerfil(comentario.autor.perfil)"
      >
        <div class="text-body2" style="white-space: pre-wrap">{{ comentario.texto }}</div>
      </q-timeline-entry>
    </q-timeline>

    <div v-else class="text-grey-6 q-py-md">
      Nenhum comentario ainda. Seja o primeiro a escrever.
    </div>
  </div>
</template>

<script setup>
import { date } from 'quasar'

defineProps({
  comentarios: { type: Array, required: true }
})

/**
 * O backend serializa Instant em UTC (ISO-8601). O `new Date` converte para o fuso do
 * navegador, que na demonstracao e America/Sao_Paulo.
 */
function formatar(instante) {
  return date.formatDate(new Date(instante), 'DD/MM/YYYY HH:mm')
}

function iconePorPerfil(perfil) {
  return perfil === 'SOLICITANTE' ? 'person' : 'support_agent'
}

function corPorPerfil(perfil) {
  if (perfil === 'ADMIN') return 'purple-6'
  return perfil === 'SOLICITANTE' ? 'blue-7' : 'teal-7'
}
</script>
