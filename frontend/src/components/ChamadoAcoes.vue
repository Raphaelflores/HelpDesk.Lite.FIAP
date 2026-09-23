<template>
  <div class="row q-gutter-sm items-center">
    <q-btn
      v-for="acao in acoes"
      :key="acao.chave"
      :color="acao.cor"
      :icon="acao.icone"
      :label="acao.rotulo"
      :loading="processando === acao.chave"
      :disable="processando !== null"
      :data-teste="`acao-${acao.chave}`"
      unelevated
      no-caps
      @click="executar(acao)"
    />

    <div v-if="acoes.length === 0" class="text-grey-6 text-caption">
      Nenhuma acao disponivel para o seu perfil neste status.
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

/**
 * Botoes de acao do chamado, filtrados por perfil + status.
 *
 * Espelha a maquina de estados do backend (secao 3 de .ai/business-rules.md). Esconder o
 * botao e so UX: quem recusa de verdade e o backend, com 403 ou 422.
 */
const props = defineProps({
  chamado: { type: Object, required: true },
  perfil: { type: String, required: true },
  usuarioId: { type: [Number, String], default: null }
})

const emit = defineEmits(['assumir', 'alterar-status'])

const processando = ref(null)

const ehAdmin = computed(() => props.perfil === 'ADMIN')
const ehAtendente = computed(() => props.perfil === 'ATENDENTE')
const ehSolicitante = computed(() => props.perfil === 'SOLICITANTE')

const souOSolicitante = computed(
  () => String(props.chamado?.solicitante?.id) === String(props.usuarioId)
)
const souOAtendente = computed(
  () => props.chamado?.atendente != null &&
    String(props.chamado.atendente.id) === String(props.usuarioId)
)

const acoes = computed(() => {
  const status = props.chamado?.status
  const disponiveis = []

  // ABERTO -> EM_ATENDIMENTO: atendente ou admin.
  if (status === 'ABERTO' && (ehAtendente.value || ehAdmin.value)) {
    disponiveis.push({
      chave: 'assumir', rotulo: 'Assumir', icone: 'assignment_ind', cor: 'primary', evento: 'assumir'
    })
  }

  // EM_ATENDIMENTO -> RESOLVIDO: o atendente responsavel, ou o admin.
  if (status === 'EM_ATENDIMENTO' && (ehAdmin.value || (ehAtendente.value && souOAtendente.value))) {
    disponiveis.push({
      chave: 'resolver', rotulo: 'Resolver', icone: 'task_alt', cor: 'teal-7',
      evento: 'alterar-status', status: 'RESOLVIDO'
    })
  }

  // RESOLVIDO -> FECHADO e RESOLVIDO -> EM_ATENDIMENTO: o solicitante dono, ou o admin.
  if (status === 'RESOLVIDO' && (ehAdmin.value || (ehSolicitante.value && souOSolicitante.value))) {
    disponiveis.push({
      chave: 'fechar', rotulo: 'Confirmar e fechar', icone: 'check_circle', cor: 'positive',
      evento: 'alterar-status', status: 'FECHADO'
    })
    disponiveis.push({
      chave: 'reabrir', rotulo: 'Reabrir', icone: 'restart_alt', cor: 'orange-8',
      evento: 'alterar-status', status: 'EM_ATENDIMENTO'
    })
  }

  // FECHADO e estado final: nenhuma acao.
  return disponiveis
})

async function executar(acao) {
  processando.value = acao.chave
  try {
    if (acao.evento === 'assumir') {
      emit('assumir')
    } else {
      emit('alterar-status', acao.status)
    }
  } finally {
    processando.value = null
  }
}
</script>
