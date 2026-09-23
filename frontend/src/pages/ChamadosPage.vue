<template>
  <q-page padding>
    <div class="pagina">
      <div class="row items-center justify-between q-mb-md">
        <div>
          <div class="text-h5">Chamados</div>
          <div class="text-caption text-grey-7">{{ legenda }}</div>
        </div>
        <q-btn
          unelevated
          no-caps
          color="primary"
          icon="add"
          label="Novo chamado"
          :to="{ name: 'novo-chamado' }"
        />
      </div>

      <ChamadoFiltros
        v-model="filtrosLocais"
        :categorias="catalogo.categorias"
        :perfil="auth.perfil"
        class="q-mb-md"
        @filtrar="aplicar"
        @limpar="limpar"
      />

      <q-table
        :rows="chamados.lista"
        :columns="colunas"
        :loading="chamados.carregando"
        row-key="id"
        flat
        bordered
        hide-pagination
        :rows-per-page-options="[0]"
        no-data-label="Nenhum chamado encontrado com esses filtros."
        @row-click="(evento, linha) => abrirDetalhe(linha)"
      >
        <template #body-cell-titulo="props">
          <q-td :props="props" class="linha-clicavel">
            <div class="text-weight-medium">{{ props.row.titulo }}</div>
            <div class="text-caption text-grey-7">#{{ props.row.id }}</div>
          </q-td>
        </template>

        <template #body-cell-status="props">
          <q-td :props="props"><StatusBadge :status="props.row.status" /></q-td>
        </template>

        <template #body-cell-prioridade="props">
          <q-td :props="props"><PrioridadeBadge :prioridade="props.row.prioridade" /></q-td>
        </template>

        <template #body-cell-sla="props">
          <q-td :props="props">
            <q-badge v-if="props.row.foraDoSla" color="negative" label="Fora do SLA" />
            <span v-else class="text-grey-7">{{ formatar(props.row.prazoSla) }}</span>
          </q-td>
        </template>

        <template #body-cell-acoes="props">
          <q-td :props="props">
            <q-btn
              flat
              dense
              no-caps
              color="primary"
              icon="open_in_new"
              label="Abrir"
              @click.stop="abrirDetalhe(props.row)"
            />
          </q-td>
        </template>
      </q-table>

      <div v-if="chamados.paginacao.totalPages > 1" class="row justify-center q-mt-md">
        <q-pagination
          :model-value="chamados.paginacao.page + 1"
          :max="chamados.paginacao.totalPages"
          :max-pages="7"
          boundary-numbers
          direction-links
          color="primary"
          @update:model-value="(pagina) => chamados.irParaPagina(pagina - 1)"
        />
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { date } from 'quasar'
import ChamadoFiltros from 'components/ChamadoFiltros.vue'
import StatusBadge from 'components/StatusBadge.vue'
import PrioridadeBadge from 'components/PrioridadeBadge.vue'
import { useAuthStore } from 'stores/auth'
import { useChamadosStore } from 'stores/chamados'
import { useCatalogoStore } from 'stores/catalogo'

const router = useRouter()
const auth = useAuthStore()
const chamados = useChamadosStore()
const catalogo = useCatalogoStore()

const filtrosLocais = ref({ ...chamados.filtros })

const colunas = [
  { name: 'titulo', label: 'Chamado', field: 'titulo', align: 'left' },
  { name: 'status', label: 'Status', field: 'status', align: 'left' },
  { name: 'prioridade', label: 'Prioridade', field: 'prioridade', align: 'left' },
  { name: 'categoria', label: 'Categoria', field: (linha) => linha.categoria?.nome, align: 'left' },
  { name: 'solicitante', label: 'Solicitante', field: (linha) => linha.solicitante?.nome, align: 'left' },
  {
    name: 'atendente',
    label: 'Atendente',
    field: (linha) => linha.atendente?.nome ?? 'Nao atribuido',
    align: 'left'
  },
  { name: 'criadoEm', label: 'Aberto em', field: 'criadoEm', align: 'left', format: (v) => formatar(v) },
  { name: 'sla', label: 'SLA', field: 'prazoSla', align: 'left' },
  { name: 'acoes', label: '', field: 'id', align: 'right' }
]

const legenda = computed(() => {
  const total = chamados.paginacao.totalElements
  const sufixo = total === 1 ? 'chamado' : 'chamados'
  return auth.isSolicitante
    ? `${total} ${sufixo} abertos por voce`
    : `${total} ${sufixo} na base`
})

function formatar(instante) {
  return instante ? date.formatDate(new Date(instante), 'DD/MM/YYYY HH:mm') : '-'
}

function abrirDetalhe(chamado) {
  router.push({ name: 'chamado-detalhe', params: { id: chamado.id } })
}

function aplicar(filtros) {
  chamados.aplicarFiltros(filtros).catch(() => {})
}

function limpar() {
  chamados.limparFiltros().catch(() => {})
}

onMounted(() => {
  catalogo.carregarCategorias().catch(() => {})
  chamados.carregarLista().catch(() => {})
})
</script>
