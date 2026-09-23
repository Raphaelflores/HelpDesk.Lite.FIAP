import { defineConfig } from 'vitest/config'
import { fileURLToPath } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { quasar, transformAssetUrls } from '@quasar/vite-plugin'

/**
 * Vitest roda fora do `quasar dev`, entao precisa dos mesmos plugins que o Quasar CLI
 * aplica: o plugin-vue para compilar os SFCs e o plugin do Quasar para o auto-import dos
 * componentes (q-btn, q-table...) e as variaveis de Sass.
 */
export default defineConfig({
  plugins: [
    vue({ template: { transformAssetUrls } }),
    quasar({ sassVariables: fileURLToPath(new URL('./src/css/quasar.variables.scss', import.meta.url)) })
  ],
  resolve: {
    alias: {
      src: fileURLToPath(new URL('./src', import.meta.url)),
      boot: fileURLToPath(new URL('./src/boot', import.meta.url)),
      components: fileURLToPath(new URL('./src/components', import.meta.url)),
      stores: fileURLToPath(new URL('./src/stores', import.meta.url))
    }
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['src/__tests__/setup.js'],
    globals: true,
    include: ['src/__tests__/**/*.spec.js'],
    server: {
      deps: { inline: ['quasar'] }
    }
  }
})
