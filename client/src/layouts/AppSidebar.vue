<template>
  <!-- Preline application sidebar; large screens only (small screens get the nav in AppHeader) -->
  <aside
    class="fixed inset-y-0 start-0 z-20 hidden w-65 flex-col border-e border-line bg-surface lg:flex"
    aria-label="Боковая навигация"
  >
    <div class="flex h-14 items-center px-6">
      <RouterLink
        to="/"
        class="inline-flex items-center gap-x-2 rounded-md text-[15px] font-semibold text-ink no-underline focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent"
      >
        <span class="inline-flex size-7 items-center justify-center rounded-lg bg-accent text-white">
          <NavIcon name="route" />
        </span>
        Маршрутизация бригад
      </RouterLink>
    </div>

    <nav class="flex-1 overflow-y-auto px-3 pt-3 pb-4" aria-label="Разделы">
      <ul class="flex flex-col gap-y-1">
        <li v-for="link in NAV_LINKS" :key="link.to">
          <RouterLink
            :to="link.to"
            class="flex items-center gap-x-3.5 rounded-lg px-2.5 py-2 text-sm no-underline focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent"
            :class="isActive(link.to) ? 'bg-accent font-medium text-white' : 'text-muted hover:bg-shift hover:text-ink'"
            :aria-current="isActive(link.to) ? 'page' : undefined"
            active-class=""
            exact-active-class=""
          >
            <NavIcon :name="link.icon" />
            {{ link.label }}
          </RouterLink>
        </li>
      </ul>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import NavIcon from "./NavIcon.vue";
import { NAV_LINKS, useActiveSection } from "./navigation";

const { isActive } = useActiveSection();
</script>
