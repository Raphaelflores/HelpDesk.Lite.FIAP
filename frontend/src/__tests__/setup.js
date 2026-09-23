/**
 * Setup do Vitest (nao e um arquivo de teste -- o glob so pega *.spec.js).
 *
 * O jsdom nao implementa `window.matchMedia`, e o modo `auto` do Dark do Quasar depende
 * dele para acompanhar o `prefers-color-scheme` do sistema. Sem este stub, qualquer
 * chamada a `Dark.set('auto')` explode no ambiente de teste -- no navegador funciona.
 */
if (typeof window.matchMedia !== 'function') {
  window.matchMedia = (consulta) => ({
    matches: false,
    media: consulta,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false
  })
}
