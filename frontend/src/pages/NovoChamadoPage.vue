<template>
  <q-page padding>
    <div class="pagina" style="max-width: 760px">
      <q-btn flat dense no-caps icon="arrow_back" label="Voltar" class="q-mb-md"
             :to="{ name: 'chamados' }" />

      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5">Novo chamado</div>
          <div class="text-caption text-grey-7">
            O chamado nasce em ABERTO e fica na fila ate um atendente assumir.
          </div>
        </q-card-section>

        <q-separator />

        <q-form ref="formulario" @submit.prevent="enviar">
          <q-card-section class="q-gutter-md">
            <q-input
              v-model="chamado.titulo"
              label="Titulo *"
              outlined
              counter
              maxlength="150"
              data-teste="campo-titulo"
              hint="Resuma o problema em uma frase"
              :rules="[
                (v) => !!v?.trim() || 'Informe um titulo',
                (v) => v.trim().length >= 5 || 'O titulo precisa de pelo menos 5 caracteres'
              ]"
            />

            <q-input
              v-model="chamado.descricao"
              type="textarea"
              label="Descricao *"
              outlined
              autogrow
              counter
              maxlength="2000"
              input-style="min-height: 120px"
              hint="O que aconteceu, desde quando, e o que voce ja tentou"
              :rules="[
                (v) => !!v?.trim() || 'Informe uma descricao',
                (v) => v.trim().length >= 10 || 'A descricao precisa de pelo menos 10 caracteres'
              ]"
            />

            <div class="row q-col-gutter-md">
              <div class="col-12 col-sm-6">
                <q-select
                  v-model="chamado.categoriaId"
                  :options="opcoesCategoria"
                  label="Categoria *"
                  outlined
                  emit-value
                  map-options
                  :loading="catalogo.carregando"
                  :rules="[(v) => v !== null || 'Escolha uma categoria']"
                />
              </div>

              <div class="col-12 col-sm-6">
                <q-select
                  v-model="chamado.prioridade"
                  :options="OPCOES_PRIORIDADE"
                  label="Prioridade *"
                  outlined
                  emit-value
                  map-options
                  :rules="[(v) => !!v || 'Escolha uma prioridade']"
                />
              </div>
            </div>

            <q-banner v-if="slaEscolhido" dense :class="$q.dark.isActive ? 'bg-blue-10' : 'bg-blue-1'">
              <template #avatar><q-icon name="schedule" color="primary" /></template>
              Essa categoria tem SLA de <b>{{ slaEscolhido }} horas</b> a partir da abertura.
            </q-banner>
          </q-card-section>

          <q-separator />

          <q-card-actions align="right" class="q-pa-md">
            <q-btn flat no-caps color="grey-8" label="Cancelar" :to="{ name: 'chamados' }" />
            <q-btn
              unelevated
              no-caps
              color="primary"
              icon="send"
              label="Abrir chamado"
              type="submit"
              :loading="enviando"
            />
          </q-card-actions>
        </q-form>
      </q-card>
    </div>
  </q-page>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useChamadosStore } from 'stores/chamados'
import { useCatalogoStore } from 'stores/catalogo'

const router = useRouter()
const $q = useQuasar()
const chamados = useChamadosStore()
const catalogo = useCatalogoStore()

const OPCOES_PRIORIDADE = [
  { label: 'Baixa', value: 'BAIXA' },
  { label: 'Media', value: 'MEDIA' },
  { label: 'Alta', value: 'ALTA' }
]

const formulario = ref(null)
const enviando = ref(false)

const chamado = reactive({
  titulo: '',
  descricao: '',
  categoriaId: null,
  prioridade: 'MEDIA'
})

const opcoesCategoria = computed(() =>
  catalogo.categorias.map((categoria) => ({
    label: `${categoria.nome} (SLA ${categoria.slaHoras}h)`,
    value: categoria.id
  }))
)

const slaEscolhido = computed(
  () => catalogo.categorias.find((c) => c.id === chamado.categoriaId)?.slaHoras ?? null
)

onMounted(() => catalogo.carregarCategorias().catch(() => {}))

async function enviar() {
  if (!(await formulario.value.validate())) {
    return
  }

  enviando.value = true
  try {
    const criado = await chamados.abrir({
      titulo: chamado.titulo.trim(),
      descricao: chamado.descricao.trim(),
      categoriaId: chamado.categoriaId,
      prioridade: chamado.prioridade
    })
    $q.notify({ type: 'positive', message: `Chamado #${criado.id} aberto.` })
    router.push({ name: 'chamado-detalhe', params: { id: criado.id } })
  } finally {
    enviando.value = false
  }
}
</script>
