<template>
  <q-page padding>
    <div class="pagina">
      <div class="row items-center justify-between q-mb-md">
        <div>
          <div class="text-h5">Categorias</div>
          <div class="text-caption text-grey-7">
            Excluir uma categoria apenas a desativa: os chamados historicos continuam
            apontando para ela.
          </div>
        </div>
        <q-btn unelevated no-caps color="primary" icon="add" label="Nova categoria"
               data-teste="nova-categoria" @click="abrirDialogo()" />
      </div>

      <q-table
        :rows="catalogo.categorias"
        :columns="colunas"
        :loading="catalogo.carregando"
        row-key="id"
        flat
        bordered
        hide-pagination
        :rows-per-page-options="[0]"
        no-data-label="Nenhuma categoria cadastrada."
      >
        <template #body-cell-ativa="props">
          <q-td :props="props">
            <q-badge :color="props.row.ativa ? 'positive' : 'grey-6'"
                     :label="props.row.ativa ? 'Ativa' : 'Inativa'" />
          </q-td>
        </template>

        <template #body-cell-acoes="props">
          <q-td :props="props">
            <q-btn flat dense round color="primary" icon="edit" @click="abrirDialogo(props.row)">
              <q-tooltip>Editar</q-tooltip>
            </q-btn>
            <q-btn v-if="props.row.ativa" flat dense round color="negative" icon="block"
                   @click="confirmarDesativacao(props.row)">
              <q-tooltip>Desativar</q-tooltip>
            </q-btn>
          </q-td>
        </template>
      </q-table>

      <q-dialog v-model="dialogoAberto">
        <q-card style="width: 420px; max-width: 92vw">
          <q-card-section>
            <div class="text-h6">{{ emEdicao.id ? 'Editar categoria' : 'Nova categoria' }}</div>
          </q-card-section>

          <q-form ref="formulario" @submit.prevent="salvar">
            <q-card-section class="q-gutter-md">
              <q-input
                v-model="emEdicao.nome"
                label="Nome *"
                outlined
                maxlength="80"
                counter
                :rules="[
                  (v) => !!v?.trim() || 'Informe o nome',
                  (v) => v.trim().length >= 2 || 'O nome precisa de pelo menos 2 caracteres'
                ]"
              />
              <q-input
                v-model.number="emEdicao.slaHoras"
                type="number"
                label="SLA em horas *"
                outlined
                hint="Prazo de atendimento contado a partir da abertura do chamado"
                :rules="[
                  (v) => (v !== null && v !== '') || 'Informe o SLA',
                  (v) => v > 0 || 'O SLA precisa ser maior que zero'
                ]"
              />
            </q-card-section>

            <q-card-actions align="right">
              <q-btn v-close-popup flat no-caps color="grey-8" label="Cancelar" />
              <q-btn unelevated no-caps color="primary" label="Salvar" type="submit"
                     :loading="salvando" />
            </q-card-actions>
          </q-form>
        </q-card>
      </q-dialog>
    </div>
  </q-page>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useQuasar } from 'quasar'
import { useCatalogoStore } from 'stores/catalogo'

const $q = useQuasar()
const catalogo = useCatalogoStore()

const colunas = [
  { name: 'nome', label: 'Nome', field: 'nome', align: 'left' },
  { name: 'slaHoras', label: 'SLA (horas)', field: 'slaHoras', align: 'left' },
  { name: 'ativa', label: 'Situacao', field: 'ativa', align: 'left' },
  { name: 'acoes', label: '', field: 'id', align: 'right' }
]

const dialogoAberto = ref(false)
const salvando = ref(false)
const formulario = ref(null)
const emEdicao = reactive({ id: null, nome: '', slaHoras: 8 })

// incluirInativas: a tela de administracao precisa enxergar tambem o que foi desativado.
onMounted(() => catalogo.carregarCategorias(true).catch(() => {}))

function abrirDialogo(categoria = null) {
  Object.assign(emEdicao, categoria
    ? { id: categoria.id, nome: categoria.nome, slaHoras: categoria.slaHoras }
    : { id: null, nome: '', slaHoras: 8 })
  dialogoAberto.value = true
}

async function salvar() {
  if (!(await formulario.value.validate())) {
    return
  }

  salvando.value = true
  try {
    await catalogo.salvarCategoria({ ...emEdicao })
    dialogoAberto.value = false
    $q.notify({ type: 'positive', message: 'Categoria salva.' })
  } finally {
    salvando.value = false
  }
}

function confirmarDesativacao(categoria) {
  $q.dialog({
    title: 'Desativar categoria',
    message: `A categoria "${categoria.nome}" deixa de aparecer em novos chamados. `
      + 'Os chamados existentes continuam nela. Confirma?',
    cancel: { flat: true, noCaps: true, label: 'Cancelar' },
    ok: { unelevated: true, noCaps: true, color: 'negative', label: 'Desativar' },
    persistent: true
  }).onOk(async () => {
    await catalogo.desativarCategoria(categoria.id)
    $q.notify({ type: 'positive', message: 'Categoria desativada.' })
  })
}
</script>
