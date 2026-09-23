<template>
  <q-page class="flex flex-center column q-gutter-md">
    <q-icon name="search_off" size="80px" color="grey-5" />
    <div class="text-h4">404</div>
    <div class="text-grey-7">Essa pagina nao existe.</div>
    <q-btn unelevated no-caps color="primary" icon="home" label="Ir para os chamados"
           :to="{ name: 'chamados' }" />
  </q-page>
</template>

<script setup>
// Rota catchAll do router.
</script>
