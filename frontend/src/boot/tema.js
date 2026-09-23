import { watchEffect } from 'vue'
import { Dark } from 'quasar'

/**
 * Tema claro/escuro da aplicacao.
 *
 * Este arquivo e o unico lugar que fala com o `Dark` do Quasar e com o `localStorage` do
 * tema -- o componente SeletorTema so chama as funcoes daqui.
 *
 * Roda como boot file para o tema ja estar aplicado quando a primeira tela pinta; se
 * ficasse so no componente, haveria um flash de tela clara a cada F5.
 */

const CHAVE = 'helpdesk.tema'

/** `auto` segue o sistema operacional e continua acompanhando se o usuario trocar. */
export const MODOS = Object.freeze({
  AUTO: 'auto',
  CLARO: 'claro',
  ESCURO: 'escuro'
})

const PARA_QUASAR = {
  [MODOS.AUTO]: 'auto',
  [MODOS.CLARO]: false,
  [MODOS.ESCURO]: true
}

/**
 * Informa ao navegador qual esquema a pagina esta usando.
 *
 * Sem isso, quem esta com o sistema no escuro e escolhe "Claro" ve legendas invisiveis: o
 * Quasar define a cor da legenda como `color-mix(in srgb, currentcolor 54%, transparent)`,
 * e o Chromium resolve esse `currentcolor` autorreferente contra o padrao do navegador --
 * que e branco sob `prefers-color-scheme: dark` -- em vez da cor herdada. Declarar o
 * `color-scheme` alinha esse padrao ao tema de verdade.
 */
function sincronizarColorScheme() {
  document.documentElement.style.colorScheme = Dark.isActive ? 'dark' : 'light'
}

/** Modo escolhido pelo usuario, ou `auto` se ele nunca escolheu. */
export function temaSalvo() {
  try {
    const salvo = localStorage.getItem(CHAVE)
    return Object.values(MODOS).includes(salvo) ? salvo : MODOS.AUTO
  } catch {
    // Navegador sem localStorage: cai no automatico. Nao e motivo para quebrar a tela.
    return MODOS.AUTO
  }
}

/** Aplica o modo no Quasar e guarda a escolha para o proximo acesso. */
export function aplicarTema(modo) {
  const escolhido = Object.values(MODOS).includes(modo) ? modo : MODOS.AUTO

  Dark.set(PARA_QUASAR[escolhido])
  sincronizarColorScheme()

  try {
    localStorage.setItem(CHAVE, escolhido)
  } catch {
    // Sem localStorage o tema vale so nesta aba.
  }

  return escolhido
}

// Boot file: uma funcao exportada por padrao basta, sem o wrapper defineBoot. Assim este
// modulo nao depende de `#q-app/wrappers` e continua importavel pelo Vitest.
export default function () {
  Dark.set(PARA_QUASAR[temaSalvo()])

  // Reage tambem quando o modo e `auto` e o usuario troca o tema do sistema com o app
  // aberto -- nesse caso o Quasar vira sozinho e so o color-scheme ficaria para tras.
  watchEffect(() => {
    // Leitura explicita para o efeito depender de isActive.
    void Dark.isActive
    sincronizarColorScheme()
  })
}
