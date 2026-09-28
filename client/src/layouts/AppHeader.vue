<template>
  <!-- Preline top bar: breadcrumb next to the sidebar on large screens; on small ones the menu button (the
       sidebar slides in with the sections) and the current section -->
  <header class="sticky top-0 z-10 flex h-14 items-center gap-x-3 border-b border-line bg-surface px-4 sm:px-6">
    <ol class="hidden items-center gap-x-2 text-sm lg:flex" aria-label="Навигационная цепочка">
      <li class="text-muted">Маршрутизация бригад</li>
      <li v-if="activeLink" class="flex items-center gap-x-2 font-medium text-ink" aria-current="page">
        <svg class="size-3.5 text-faint" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <path d="m9 18 6-6-6-6" />
        </svg>
        {{ activeLink.label }}
      </li>
    </ol>

    <div class="flex min-w-0 flex-1 items-center gap-x-3 lg:hidden">
      <button
        type="button"
        class="-ms-1.5 inline-flex size-9 shrink-0 cursor-pointer items-center justify-center rounded-lg text-ink hover:bg-shift focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent"
        aria-label="Открыть меню"
        aria-controls="app-sidebar"
        :aria-expanded="sidebarOpen"
        @click="emit('openSidebar')"
      >
        <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
          <path d="M4 6h16M4 12h16M4 18h16" />
        </svg>
      </button>

      <RouterLink
        to="/"
        class="inline-flex shrink-0 items-center rounded-lg bg-accent p-1.5 text-white focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2"
        aria-label="Маршрутизация бригад"
      >
        <NavIcon name="route" />
      </RouterLink>

      <!-- where the page is, the sections themselves are in the menu -->
      <span v-if="activeLink" class="truncate text-sm font-medium text-ink" aria-current="page">{{ activeLink.label }}</span>
    </div>

    <!-- top right corner on every screen size: a new request from any page (preselected in the region the
         schedule or map shows, if one was chosen), then the theme -->
    <div class="ms-auto flex shrink-0 items-center gap-x-2">
      <RequestCreateAction
        :regions="requestCreateStore.regions"
        :region-id="regionsStore.regionId"
        :open="requestCreateStore.open"
        :pending="requestCreateStore.pending"
        :error="requestCreateStore.error"
        @open="requestCreateStore.openDialog()"
        @close="requestCreateStore.closeDialog()"
        @submit="requestCreateStore.create($event)"
      />
      <ThemeToggle />
    </div>
  </header>
</template>

<script setup lang="ts">
import RequestCreateAction from "@/features/requests/components/RequestCreateAction.vue";
import { useRequestCreateStore } from "@/features/requests/stores/requestCreate";
import { useRegionsStore } from "@/features/timeline/stores/regions";
import NavIcon from "./NavIcon.vue";
import ThemeToggle from "./ThemeToggle.vue";
import { useActiveSection } from "./navigation";

defineProps<{
  /** the sidebar menu is open (small screens) */
  sidebarOpen: boolean;
}>();

const emit = defineEmits<{
  openSidebar: [];
}>();

const { activeLink } = useActiveSection();

const requestCreateStore = useRequestCreateStore();
// only read: the schedule and map pages keep it; its load() would write ?region= into the URL
const regionsStore = useRegionsStore();
</script>
