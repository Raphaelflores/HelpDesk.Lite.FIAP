<template>
  <q-card flat bordered class="q-pa-md">
    <div class="row q-col-gutter-md items-center">
      <div class="col-12 col-sm-6 col-md-3">
        <q-select
          v-model="local.status"
          :options="OPCOES_STATUS"
          label="Status"
          emit-value
          map-options
          clearable
          outlined
          dense
          data-teste="filtro-status"
        />
      </div>

      <div class="col-12 col-sm-6 col-md-3">
        <q-select
          v-model="local.prioridade"
          :options="OPCOES_PRIORIDADE"
          label="Prioridade"
          emit-value
          map-options
          clearable
          outlined
          dense
        />
      </div>

      <div class="col-12 col-sm-6 col-md-3">
        <q-select
          v-model="local.categoriaId"
          :options="opcoesCategoria"
          label="Categoria"
          emit-value
          map-options
          clearable
          outlined
          dense
        />
      </div>

      <div class="col-12 col-sm-6 col-md-3 row items-center justify-between">
        <q-toggle
          v-model="local.meus"
          :label="rotuloMeus"
          color="primary"
          dense
        />
      </div>
    </div>

    <div class="row justify-end q-gutter-sm q-mt-md">
      <q-btn flat no-caps color="grey-8" icon="clear" label="Limpar" @click="limpar" />
      <q-btn unelevated no-caps color="primary" icon="search" label="Filtrar" @click="aplicar" />
    </div>
  </q-card>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Object, required: true },
  categorias: { type: Array, default: () => [] },
  perfil: { type: String, required: true }
})

const emit = defineEmits(['update:modelValue', 'filtrar', 'limpar'])

const OPCOES_STATUS = [
  { label: 'Aberto', value: 'ABERTO' },
  { label: 'Em atendimento', value: 'EM_ATENDIMENTO' },
  { label: 'Resolvido', value: 'RESOLVIDO' },
  { label: 'Fechado', value: 'FECHADO' }
]

const OPCOES_PRIORIDADE = [
  { label: 'Baixa', value: 'BAIXA' },
  { label: 'Media', value: 'MEDIA' },
  { label: 'Alta', value: 'ALTA' }
]

const local = reactive({ ...props.modelValue })

watch(() => props.modelValue, (novo) => Object.assign(local, novo), { deep: true })

const opcoesCategoria = computed(() =>
  props.categorias.map((categoria) => ({ label: categoria.nome, value: categoria.id }))
)

/** Para o solicitante "meus" nao muda nada: ele ja so enxerga os proprios chamados. */
const rotuloMeus = computed(() =>
  props.perfil === 'SOLICITANTE' ? 'Somente os meus' : 'Somente os que eu atendo'
)

function aplicar() {
  emit('update:modelValue', { ...local })
  emit('filtrar', { ...local })
}

function limpar() {
  Object.assign(local, { status: null, prioridade: null, categoriaId: null, meus: false })
  emit('update:modelValue', { ...local })
  emit('limpar')
}
</script>
