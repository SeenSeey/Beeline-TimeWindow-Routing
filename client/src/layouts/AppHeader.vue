<template>
  <!-- Preline top bar: breadcrumb next to the sidebar on large screens, the section nav on small ones -->
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

    <div class="flex min-w-0 flex-1 items-center gap-x-4 lg:hidden">
      <RouterLink
        to="/"
        class="inline-flex shrink-0 items-center rounded-lg bg-accent p-1.5 text-white focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2"
        aria-label="Маршрутизация бригад"
      >
        <NavIcon name="route" />
      </RouterLink>

      <nav class="flex min-w-0 items-center gap-x-1 overflow-x-auto" aria-label="Разделы">
        <RouterLink
          v-for="link in NAV_LINKS"
          :key="link.to"
          :to="link.to"
          class="inline-flex items-center gap-x-2 rounded-lg px-2.5 py-1.5 text-sm whitespace-nowrap no-underline focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent"
          :class="isActive(link.to) ? 'bg-accent font-medium text-white' : 'text-muted hover:bg-shift hover:text-ink'"
          :aria-current="isActive(link.to) ? 'page' : undefined"
          active-class=""
          exact-active-class=""
        >
          <NavIcon :name="link.icon" />
          {{ link.label }}
        </RouterLink>
      </nav>
    </div>

    <!-- top right corner on every screen size -->
    <ThemeToggle class="ms-auto" />
  </header>
</template>

<script setup lang="ts">
import NavIcon from "./NavIcon.vue";
import ThemeToggle from "./ThemeToggle.vue";
import { NAV_LINKS, useActiveSection } from "./navigation";

const { isActive, activeLink } = useActiveSection();
</script>
