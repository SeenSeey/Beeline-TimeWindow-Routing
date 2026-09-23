<template>
  <!-- `board` scopes the design tokens (global.css) for every page -->
  <div class="board flex min-h-screen flex-col bg-canvas font-(family-name:--sans) text-sm leading-[1.45] text-ink">
    <AppHeader />

    <!-- grid so the page stretches to the remaining height -->
    <main class="grid flex-1">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import AppHeader from "./AppHeader.vue";
</script>
