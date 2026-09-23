<template>
  <q-page padding>
    <div class="pagina">
      <div class="row items-center justify-between q-mb-md">
        <div>
          <div class="text-h5">Usuarios</div>
          <div class="text-caption text-grey-7">
            Usuarios sao desativados, nunca excluidos: os chamados precisam continuar
            sabendo quem os abriu.
          </div>
        </div>
        <q-btn unelevated no-caps color="primary" icon="add" label="Novo usuario"
               data-teste="novo-usuario" @click="abrirDialogo()" />
      </div>

      <q-table
        :rows="catalogo.usuarios"
        :columns="colunas"
        :loading="catalogo.carregando"
        row-key="id"
        flat
        bordered
        hide-pagination
        :rows-per-page-options="[0]"
        no-data-label="Nenhum usuario cadastrado."
      >
        <template #body-cell-perfil="props">
          <q-td :props="props">
            <q-badge :color="CORES[props.row.perfil]" :label="ROTULOS[props.row.perfil]" />
          </q-td>
        </template>

        <template #body-cell-ativo="props">
          <q-td :props="props">
            <q-badge :color="props.row.ativo ? 'positive' : 'grey-6'"
                     :label="props.row.ativo ? 'Ativo' : 'Inativo'" />
          </q-td>
        </template>

        <template #body-cell-acoes="props">
          <q-td :props="props">
            <q-btn flat dense round color="primary" icon="edit" @click="abrirDialogo(props.row)">
              <q-tooltip>Editar</q-tooltip>
            </q-btn>
            <q-btn
              v-if="props.row.ativo"
              flat
              dense
              round
              color="negative"
              icon="person_off"
              :disable="props.row.id === auth.usuario?.id"
              @click="confirmarDesativacao(props.row)"
            >
              <q-tooltip>
                {{ props.row.id === auth.usuario?.id
                  ? 'Voce nao pode desativar o proprio usuario'
                  : 'Desativar' }}
              </q-tooltip>
            </q-btn>
          </q-td>
        </template>
      </q-table>

      <q-dialog v-model="dialogoAberto">
        <q-card style="width: 460px; max-width: 92vw">
          <q-card-section>
            <div class="text-h6">{{ emEdicao.id ? 'Editar usuario' : 'Novo usuario' }}</div>
          </q-card-section>

          <q-form ref="formulario" @submit.prevent="salvar">
            <q-card-section class="q-gutter-md">
              <q-input
                v-model="emEdicao.nome"
                label="Nome *"
                outlined
                maxlength="120"
                counter
                :rules="[
                  (v) => !!v?.trim() || 'Informe o nome',
                  (v) => v.trim().length >= 3 || 'O nome precisa de pelo menos 3 caracteres'
                ]"
              />
              <q-input
                v-model="emEdicao.email"
                type="email"
                label="E-mail *"
                outlined
                maxlength="150"
                :rules="[
                  (v) => !!v?.trim() || 'Informe o e-mail',
                  (v) => /.+@.+\..+/.test(v) || 'E-mail invalido'
                ]"
              />
              <q-select
                v-model="emEdicao.perfil"
                :options="OPCOES_PERFIL"
                label="Perfil *"
                outlined
                emit-value
                map-options
                :rules="[(v) => !!v || 'Escolha um perfil']"
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
import { useAuthStore } from 'stores/auth'
import { useCatalogoStore } from 'stores/catalogo'

const $q = useQuasar()
const auth = useAuthStore()
const catalogo = useCatalogoStore()

const ROTULOS = { SOLICITANTE: 'Solicitante', ATENDENTE: 'Atendente', ADMIN: 'Admin' }
const CORES = { SOLICITANTE: 'blue-7', ATENDENTE: 'teal-7', ADMIN: 'purple-6' }

const OPCOES_PERFIL = [
  { label: 'Solicitante', value: 'SOLICITANTE' },
  { label: 'Atendente', value: 'ATENDENTE' },
  { label: 'Administrador', value: 'ADMIN' }
]

const colunas = [
  { name: 'nome', label: 'Nome', field: 'nome', align: 'left' },
  { name: 'email', label: 'E-mail', field: 'email', align: 'left' },
  { name: 'perfil', label: 'Perfil', field: 'perfil', align: 'left' },
  { name: 'ativo', label: 'Situacao', field: 'ativo', align: 'left' },
  { name: 'acoes', label: '', field: 'id', align: 'right' }
]

const dialogoAberto = ref(false)
const salvando = ref(false)
const formulario = ref(null)
const emEdicao = reactive({ id: null, nome: '', email: '', perfil: 'SOLICITANTE' })

onMounted(() => catalogo.carregarUsuarios().catch(() => {}))

function abrirDialogo(usuario = null) {
  Object.assign(emEdicao, usuario
    ? { id: usuario.id, nome: usuario.nome, email: usuario.email, perfil: usuario.perfil }
    : { id: null, nome: '', email: '', perfil: 'SOLICITANTE' })
  dialogoAberto.value = true
}

async function salvar() {
  if (!(await formulario.value.validate())) {
    return
  }

  salvando.value = true
  try {
    await catalogo.salvarUsuario({ ...emEdicao })
    dialogoAberto.value = false
    $q.notify({ type: 'positive', message: 'Usuario salvo.' })
  } finally {
    salvando.value = false
  }
}

function confirmarDesativacao(usuario) {
  $q.dialog({
    title: 'Desativar usuario',
    message: `"${usuario.nome}" nao vai mais conseguir entrar no sistema. `
      + 'Os chamados dele continuam no historico. Confirma?',
    cancel: { flat: true, noCaps: true, label: 'Cancelar' },
    ok: { unelevated: true, noCaps: true, color: 'negative', label: 'Desativar' },
    persistent: true
  }).onOk(async () => {
    await catalogo.desativarUsuario(usuario.id)
    $q.notify({ type: 'positive', message: 'Usuario desativado.' })
  })
}
</script>
