<template>
  <!-- `board` scopes the design tokens (global.css) for every page -->
  <div class="board min-h-screen bg-canvas font-(family-name:--sans) text-sm leading-normal text-ink antialiased">
    <AppSidebar />

    <!-- the content column sits to the right of the fixed sidebar on large screens -->
    <div class="flex min-h-screen flex-col lg:ps-65">
      <AppHeader />

      <!-- grid so the page stretches to the remaining height -->
      <main class="grid flex-1">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import AppHeader from "./AppHeader.vue";
import AppSidebar from "./AppSidebar.vue";
</script>
