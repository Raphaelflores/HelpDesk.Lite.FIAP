/**
 * Configuracao do Quasar CLI (Vite).
 * Docs: https://v2.quasar.dev/quasar-cli-vite/quasar-config-file
 */
export default function () {
  return {
    boot: ['tema', 'axios'],

    css: ['app.scss'],

    extras: ['roboto-font', 'material-icons'],

    build: {
      target: {
        browser: ['es2022', 'firefox115', 'chrome115', 'safari14'],
        node: 'node20'
      },
      vueRouterMode: 'history',
      // Unico lugar que define a URL do backend. O src/services/api.js le daqui.
      env: {
        API_BASE_URL: process.env.API_BASE_URL || 'http://localhost:8080/api'
      }
    },

    devServer: {
      port: 9000,
      open: false
    },

    framework: {
      config: {
        notify: {
          position: 'top-right',
          timeout: 4000,
          actions: [{ icon: 'close', color: 'white', round: true }]
        }
      },
      plugins: ['Notify', 'Dialog', 'Loading']
    }
  }
}
