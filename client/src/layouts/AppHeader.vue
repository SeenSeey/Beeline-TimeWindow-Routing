<template>
  <header class="sticky top-0 z-10 border-b border-line bg-surface">
    <div class="mx-auto flex h-14 max-w-[1440px] items-center gap-8 px-5">
      <RouterLink
        to="/"
        class="shrink-0 text-[15px] font-semibold tracking-[-0.01em] text-ink no-underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
      >
        Маршрутизация бригад
      </RouterLink>

      <nav class="-mb-px flex h-full min-w-0 items-stretch gap-1 overflow-x-auto" aria-label="Разделы">
        <RouterLink
          v-for="link in LINKS"
          :key="link.to"
          :to="link.to"
          class="flex items-center border-b-2 px-3 whitespace-nowrap no-underline hover:text-ink focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-accent motion-safe:transition-colors motion-safe:duration-[120ms]"
          :class="isActive(link.to) ? 'border-accent font-medium text-ink' : 'border-transparent text-muted'"
          :aria-current="isActive(link.to) ? 'page' : undefined"
          active-class=""
          exact-active-class=""
        >
          {{ link.label }}
        </RouterLink>
      </nav>
    </div>
  </header>
</template>

<script setup lang="ts">
import { useRoute } from "vue-router";

const route = useRoute();

/** A section stays active on its nested pages too (/requests/42 belongs to /requests) */
const isActive = (sectionPath: string): boolean =>
  route.path === sectionPath || route.path.startsWith(`${sectionPath}/`);

const LINKS = [
  { to: "/map", label: "Карта" },
  { to: "/schedule", label: "Расписание" },
  { to: "/requests", label: "Заявки" },
  { to: "/crews", label: "Исполнители" },
] as const;
</script>
