<template>
  <!-- small screens: the page behind the open menu is dimmed; a click on it closes the menu -->
  <div
    v-if="open"
    class="fixed inset-0 z-30 bg-black/40 lg:hidden"
    aria-hidden="true"
    @click="emit('close')"
  ></div>

  <!-- Preline application sidebar: always shown on large screens; on small ones a menu sliding in from the
       start edge, out of view (and out of the tab order: invisible) while closed -->
  <aside
    id="app-sidebar"
    class="fixed inset-y-0 start-0 z-40 flex w-65 flex-col border-e border-line bg-surface lg:z-20 lg:translate-x-0 motion-safe:transition-[translate,visibility] motion-safe:duration-200"
    :class="open ? 'max-lg:shadow-xl' : 'max-lg:invisible max-lg:-translate-x-full'"
    aria-label="Боковая навигация"
  >
    <div class="flex h-14 items-center justify-between gap-x-2 px-6 max-lg:pe-3">
      <RouterLink
        to="/"
        class="inline-flex items-center gap-x-2 rounded-md text-[15px] font-semibold text-ink no-underline focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent"
      >
        <span class="inline-flex size-7 items-center justify-center rounded-lg bg-accent text-white">
          <NavIcon name="route" />
        </span>
        Маршрутизация бригад
      </RouterLink>

      <!-- small screens: close the menu -->
      <button
        type="button"
        class="inline-flex size-9 shrink-0 cursor-pointer items-center justify-center rounded-lg text-muted hover:bg-shift hover:text-ink focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent lg:hidden"
        aria-label="Закрыть меню"
        @click="emit('close')"
      >
        <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
          <path d="M18 6 6 18M6 6l12 12" />
        </svg>
      </button>
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
            @click="emit('close')"
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
import { onBeforeUnmount, onMounted } from "vue";
import NavIcon from "./NavIcon.vue";
import { NAV_LINKS, useActiveSection } from "./navigation";

const props = defineProps<{
  /** open as a menu (small screens); large screens always show it */
  open: boolean;
}>();

const emit = defineEmits<{
  close: [];
}>();

const { isActive } = useActiveSection();

function onKeyDown(event: KeyboardEvent): void {
  if (event.key === "Escape" && props.open) emit("close");
}

onMounted(() => document.addEventListener("keydown", onKeyDown));
onBeforeUnmount(() => document.removeEventListener("keydown", onKeyDown));
</script>
