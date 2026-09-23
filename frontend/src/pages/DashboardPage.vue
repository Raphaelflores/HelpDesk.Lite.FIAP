<template>
  <q-page padding>
    <div class="pagina">
      <div class="row items-center justify-between q-mb-md">
        <div>
          <div class="text-h5">Dashboard</div>
          <div class="text-caption text-grey-7">
            Indicadores de toda a base de chamados.
          </div>
        </div>
        <q-btn
          flat
          no-caps
          color="primary"
          icon="refresh"
          label="Atualizar"
          :loading="dashboard.carregando"
          @click="carregar"
        />
      </div>

      <template v-if="resumo">
        <div class="row q-col-gutter-md q-mb-md">
          <div v-for="cartao in cartoes" :key="cartao.rotulo" class="col-6 col-md">
            <q-card flat bordered class="cartao-indicador">
              <q-card-section>
                <div class="row items-center q-gutter-sm">
                  <q-icon :name="cartao.icone" :color="cartao.cor" size="26px" />
                  <div class="text-caption text-grey-7">{{ cartao.rotulo }}</div>
                </div>
                <div class="text-h4 q-mt-sm" :class="`text-${cartao.cor}`">{{ cartao.valor }}</div>
              </q-card-section>
            </q-card>
          </div>
        </div>

        <div class="row q-col-gutter-md q-mb-md">
          <div class="col-12 col-md-6">
            <q-card flat bordered>
              <q-card-section>
                <div class="text-subtitle1">Chamados por prioridade</div>
                <q-list dense class="q-mt-sm">
                  <q-item v-for="(total, prioridade) in resumo.totalPorPrioridade" :key="prioridade">
                    <q-item-section avatar><PrioridadeBadge :prioridade="prioridade" /></q-item-section>
                    <q-item-section><q-linear-progress
                      :value="proporcao(total)" size="10px" rounded color="primary" class="q-mt-xs" /></q-item-section>
                    <q-item-section side class="text-weight-medium">{{ total }}</q-item-section>
                  </q-item>
                </q-list>
              </q-card-section>
            </q-card>
          </div>

          <div class="col-12 col-md-6">
            <q-card flat bordered>
              <q-card-section>
                <div class="text-subtitle1">Tempo medio de resolucao</div>
                <div class="text-h3 text-primary q-mt-sm">
                  {{ resumo.tempoMedioResolucaoHoras }}<span class="text-h6 text-grey-7"> h</span>
                </div>
                <div class="text-caption text-grey-7 q-mt-sm">
                  Media de (resolvido em - aberto em) sobre os chamados ja resolvidos ou fechados.
                </div>
              </q-card-section>
            </q-card>
          </div>
        </div>

        <q-card flat bordered>
          <q-card-section class="row items-center justify-between">
            <div class="text-subtitle1">Chamados fora do SLA</div>
            <q-badge :color="resumo.foraDoSla > 0 ? 'negative' : 'positive'"
                     :label="`${resumo.foraDoSla} chamado(s)`" />
          </q-card-section>

          <q-separator />

          <q-table
            :rows="resumo.chamadosForaDoSla"
            :columns="colunas"
            row-key="id"
            flat
            hide-pagination
            :rows-per-page-options="[0]"
            no-data-label="Nenhum chamado fora do SLA. Tudo em dia."
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
          </q-table>
        </q-card>
      </template>
    </div>
  </q-page>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { date } from 'quasar'
import StatusBadge from 'components/StatusBadge.vue'
import PrioridadeBadge from 'components/PrioridadeBadge.vue'
import { useDashboardStore } from 'stores/dashboard'

const router = useRouter()
const dashboard = useDashboardStore()

const resumo = computed(() => dashboard.resumo)

const cartoes = computed(() => {
  if (!resumo.value) return []
  const status = resumo.value.totalPorStatus
  return [
    { rotulo: 'Total', valor: resumo.value.totalChamados, icone: 'inbox', cor: 'primary' },
    { rotulo: 'Abertos', valor: status.ABERTO, icone: 'fiber_new', cor: 'blue-7' },
    { rotulo: 'Em atendimento', valor: status.EM_ATENDIMENTO, icone: 'engineering', cor: 'orange-8' },
    { rotulo: 'Resolvidos', valor: status.RESOLVIDO, icone: 'task_alt', cor: 'teal-7' },
    { rotulo: 'Fechados', valor: status.FECHADO, icone: 'check_circle', cor: 'grey-7' },
    { rotulo: 'Fora do SLA', valor: resumo.value.foraDoSla, icone: 'schedule', cor: 'negative' }
  ]
})

const colunas = [
  { name: 'titulo', label: 'Chamado', field: 'titulo', align: 'left' },
  { name: 'status', label: 'Status', field: 'status', align: 'left' },
  { name: 'prioridade', label: 'Prioridade', field: 'prioridade', align: 'left' },
  { name: 'categoria', label: 'Categoria', field: (l) => l.categoria?.nome, align: 'left' },
  {
    name: 'prazoSla',
    label: 'Venceu em',
    field: 'prazoSla',
    align: 'left',
    format: (v) => date.formatDate(new Date(v), 'DD/MM/YYYY HH:mm')
  }
]

function proporcao(total) {
  const geral = resumo.value?.totalChamados ?? 0
  return geral === 0 ? 0 : total / geral
}

function abrirDetalhe(chamado) {
  router.push({ name: 'chamado-detalhe', params: { id: chamado.id } })
}

function carregar() {
  dashboard.carregar().catch(() => {})
}

onMounted(carregar)
</script>
