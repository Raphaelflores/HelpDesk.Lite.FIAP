import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { Quasar } from 'quasar'
import ChamadoAcoes from 'components/ChamadoAcoes.vue'

/**
 * Os botoes visiveis precisam bater com a maquina de estados do backend: mostrar um botao
 * que resultaria em 403 ou 422 e pior do que nao mostrar nada.
 */

const ANA = { id: 1, nome: 'Ana', perfil: 'SOLICITANTE' }
const BRUNO = { id: 2, nome: 'Bruno', perfil: 'ATENDENTE' }
const OUTRO_ATENDENTE = { id: 8, nome: 'Eva', perfil: 'ATENDENTE' }

function chamado(status, { atendente = null, solicitante = ANA } = {}) {
  return { id: 10, status, solicitante, atendente }
}

function montar(chamadoDoTeste, perfil, usuarioId) {
  return mount(ChamadoAcoes, {
    props: { chamado: chamadoDoTeste, perfil, usuarioId },
    global: { plugins: [Quasar] }
  })
}

/** Chaves das acoes renderizadas, na ordem em que aparecem. */
function acoesVisiveis(wrapper) {
  return wrapper.findAll('[data-teste^="acao-"]')
    .map((botao) => botao.attributes('data-teste').replace('acao-', ''))
}

describe('ChamadoAcoes', () => {
  describe('status ABERTO', () => {
    it('mostra Assumir para o atendente', () => {
      const wrapper = montar(chamado('ABERTO'), 'ATENDENTE', BRUNO.id)

      expect(acoesVisiveis(wrapper)).toEqual(['assumir'])
    })

    it('mostra Assumir para o admin', () => {
      const wrapper = montar(chamado('ABERTO'), 'ADMIN', 3)

      expect(acoesVisiveis(wrapper)).toEqual(['assumir'])
    })

    it('nao mostra nenhuma acao para o solicitante, nem no proprio chamado', () => {
      const wrapper = montar(chamado('ABERTO'), 'SOLICITANTE', ANA.id)

      expect(acoesVisiveis(wrapper)).toEqual([])
      expect(wrapper.text()).toContain('Nenhuma acao disponivel')
    })
  })

  describe('status EM_ATENDIMENTO', () => {
    it('mostra Resolver para o atendente responsavel', () => {
      const wrapper = montar(chamado('EM_ATENDIMENTO', { atendente: BRUNO }), 'ATENDENTE', BRUNO.id)

      expect(acoesVisiveis(wrapper)).toEqual(['resolver'])
    })

    it('esconde Resolver do atendente que nao e o responsavel', () => {
      const wrapper = montar(
        chamado('EM_ATENDIMENTO', { atendente: BRUNO }), 'ATENDENTE', OUTRO_ATENDENTE.id)

      expect(acoesVisiveis(wrapper)).toEqual([])
    })

    it('mostra Resolver para o admin mesmo sem ser o responsavel', () => {
      const wrapper = montar(chamado('EM_ATENDIMENTO', { atendente: BRUNO }), 'ADMIN', 3)

      expect(acoesVisiveis(wrapper)).toEqual(['resolver'])
    })

    it('nao mostra acao para o solicitante enquanto o chamado esta em atendimento', () => {
      const wrapper = montar(chamado('EM_ATENDIMENTO', { atendente: BRUNO }), 'SOLICITANTE', ANA.id)

      expect(acoesVisiveis(wrapper)).toEqual([])
    })
  })

  describe('status RESOLVIDO', () => {
    it('mostra Fechar e Reabrir para o solicitante dono', () => {
      const wrapper = montar(chamado('RESOLVIDO', { atendente: BRUNO }), 'SOLICITANTE', ANA.id)

      expect(acoesVisiveis(wrapper)).toEqual(['fechar', 'reabrir'])
    })

    it('esconde as acoes de outro solicitante', () => {
      const wrapper = montar(chamado('RESOLVIDO', { atendente: BRUNO }), 'SOLICITANTE', 99)

      expect(acoesVisiveis(wrapper)).toEqual([])
    })

    it('nao mostra acao para o atendente: fechar e reabrir sao do solicitante', () => {
      const wrapper = montar(chamado('RESOLVIDO', { atendente: BRUNO }), 'ATENDENTE', BRUNO.id)

      expect(acoesVisiveis(wrapper)).toEqual([])
    })

    it('mostra Fechar e Reabrir para o admin', () => {
      const wrapper = montar(chamado('RESOLVIDO', { atendente: BRUNO }), 'ADMIN', 3)

      expect(acoesVisiveis(wrapper)).toEqual(['fechar', 'reabrir'])
    })
  })

  describe('status FECHADO', () => {
    it.each(['SOLICITANTE', 'ATENDENTE', 'ADMIN'])(
      'nao mostra nenhuma acao para o perfil %s -- FECHADO e estado final',
      (perfil) => {
        const wrapper = montar(chamado('FECHADO', { atendente: BRUNO }), perfil, ANA.id)

        expect(acoesVisiveis(wrapper)).toEqual([])
      }
    )
  })

  describe('eventos', () => {
    it('emite assumir ao clicar no botao', async () => {
      const wrapper = montar(chamado('ABERTO'), 'ATENDENTE', BRUNO.id)

      await wrapper.find('[data-teste="acao-assumir"]').trigger('click')

      expect(wrapper.emitted('assumir')).toHaveLength(1)
    })

    it('emite alterar-status com RESOLVIDO ao clicar em Resolver', async () => {
      const wrapper = montar(chamado('EM_ATENDIMENTO', { atendente: BRUNO }), 'ATENDENTE', BRUNO.id)

      await wrapper.find('[data-teste="acao-resolver"]').trigger('click')

      expect(wrapper.emitted('alterar-status')[0]).toEqual(['RESOLVIDO'])
    })

    it('emite alterar-status com FECHADO e com EM_ATENDIMENTO', async () => {
      const wrapper = montar(chamado('RESOLVIDO', { atendente: BRUNO }), 'SOLICITANTE', ANA.id)

      await wrapper.find('[data-teste="acao-fechar"]').trigger('click')
      await wrapper.find('[data-teste="acao-reabrir"]').trigger('click')

      expect(wrapper.emitted('alterar-status')[0]).toEqual(['FECHADO'])
      expect(wrapper.emitted('alterar-status')[1]).toEqual(['EM_ATENDIMENTO'])
    })
  })

  it('compara ids como texto, para nao depender do tipo vindo da rota', () => {
    const wrapper = montar(chamado('RESOLVIDO', { atendente: BRUNO }), 'SOLICITANTE', '1')

    expect(acoesVisiveis(wrapper)).toEqual(['fechar', 'reabrir'])
  })
})
