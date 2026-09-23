import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { Dark, Quasar } from 'quasar'
import SeletorTema from 'components/SeletorTema.vue'
import { MODOS, aplicarTema, temaSalvo } from 'boot/tema'

const CHAVE = 'helpdesk.tema'

let wrapper = null

/**
 * O q-menu so existe no DOM depois de aberto, e e renderizado via portal em document.body
 * -- por isso o `attachTo` e as buscas em `document`, em vez de `wrapper.find`.
 */
async function abrirMenu() {
  wrapper = mount(SeletorTema, { global: { plugins: [Quasar] }, attachTo: document.body })
  await wrapper.find('[data-teste="seletor-tema"]').trigger('click')
  await new Promise((resolve) => setTimeout(resolve, 0))
  return wrapper
}

function opcao(modo) {
  return document.querySelector(`[data-teste="tema-${modo}"]`)
}

async function escolher(modo) {
  opcao(modo).dispatchEvent(new MouseEvent('click', { bubbles: true }))
  await new Promise((resolve) => setTimeout(resolve, 0))
}

describe('tema claro/escuro', () => {
  beforeEach(() => {
    localStorage.clear()
    Dark.set(false)
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = null
    document.body.innerHTML = ''
  })

  describe('boot/tema', () => {
    it('comeca em automatico quando o usuario nunca escolheu', () => {
      expect(temaSalvo()).toBe(MODOS.AUTO)
    })

    it('liga o modo escuro e guarda a escolha', () => {
      aplicarTema(MODOS.ESCURO)

      expect(Dark.isActive).toBe(true)
      expect(localStorage.getItem(CHAVE)).toBe(MODOS.ESCURO)
      expect(temaSalvo()).toBe(MODOS.ESCURO)
    })

    it('volta para o claro', () => {
      aplicarTema(MODOS.ESCURO)
      aplicarTema(MODOS.CLARO)

      expect(Dark.isActive).toBe(false)
      expect(temaSalvo()).toBe(MODOS.CLARO)
    })

    it('trata valor invalido no localStorage como automatico', () => {
      localStorage.setItem(CHAVE, 'roxo')

      expect(temaSalvo()).toBe(MODOS.AUTO)
    })

    it('ignora um modo desconhecido em vez de quebrar', () => {
      expect(aplicarTema('roxo')).toBe(MODOS.AUTO)
      expect(temaSalvo()).toBe(MODOS.AUTO)
    })

    it('declara o color-scheme da pagina junto com o tema', () => {
      // Sem isso, quem usa o sistema no escuro e escolhe "Claro" ve legendas brancas
      // sobre fundo branco: o Quasar monta a cor da legenda com color-mix sobre
      // currentcolor, e o navegador resolve isso contra o padrao do esquema do SO.
      aplicarTema(MODOS.ESCURO)
      expect(document.documentElement.style.colorScheme).toBe('dark')

      aplicarTema(MODOS.CLARO)
      expect(document.documentElement.style.colorScheme).toBe('light')
    })
  })

  describe('SeletorTema', () => {
    it('oferece as tres opcoes ao abrir o menu', async () => {
      await abrirMenu()

      expect(opcao(MODOS.AUTO)).not.toBeNull()
      expect(opcao(MODOS.CLARO)).not.toBeNull()
      expect(opcao(MODOS.ESCURO)).not.toBeNull()
    })

    it('aplica o modo escuro ao escolher a opcao', async () => {
      await abrirMenu()

      await escolher(MODOS.ESCURO)

      expect(Dark.isActive).toBe(true)
      expect(localStorage.getItem(CHAVE)).toBe(MODOS.ESCURO)
    })

    it('aplica o modo claro ao escolher a opcao', async () => {
      aplicarTema(MODOS.ESCURO)
      await abrirMenu()

      await escolher(MODOS.CLARO)

      expect(Dark.isActive).toBe(false)
      expect(localStorage.getItem(CHAVE)).toBe(MODOS.CLARO)
    })

    it('mostra o icone do tema que esta valendo, nao o do modo escolhido', async () => {
      await abrirMenu()
      expect(wrapper.find('[data-teste="seletor-tema"] .q-icon').text()).toBe('light_mode')

      await escolher(MODOS.ESCURO)

      expect(wrapper.find('[data-teste="seletor-tema"] .q-icon').text()).toBe('dark_mode')
    })

    it('nasce com a escolha ja gravada marcada', async () => {
      localStorage.setItem(CHAVE, MODOS.ESCURO)

      await abrirMenu()

      expect(opcao(MODOS.ESCURO).className).toContain('text-primary')
      expect(opcao(MODOS.CLARO).className).not.toContain('text-primary')
    })
  })
})
