<template>
  <!-- `board` scopes the design tokens (global.css) for every page -->
  <div class="board min-h-screen bg-canvas font-(family-name:--sans) text-sm leading-normal text-ink antialiased">
    <!-- fixed on large screens; on small ones a menu the header's button slides in -->
    <AppSidebar :open="sidebarOpen" @close="sidebarOpen = false" />

    <!-- the content column sits to the right of the fixed sidebar on large screens -->
    <div class="flex min-h-screen flex-col lg:ps-65">
      <AppHeader :sidebar-open="sidebarOpen" @open-sidebar="sidebarOpen = true" />

      <!-- grid so the page stretches to the remaining height -->
      <main class="grid flex-1">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { useRoute } from "vue-router";
import AppHeader from "./AppHeader.vue";
import AppSidebar from "./AppSidebar.vue";

/** the sidebar is open as a menu (small screens; on large ones it is always shown) */
const sidebarOpen = ref(false);

// picking a section (or any other navigation) closes the menu
const route = useRoute();
watch(
  () => route.path,
  () => (sidebarOpen.value = false),
);
</script>
