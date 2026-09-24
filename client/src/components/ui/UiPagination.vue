<template>
  <!-- Preline pagination: borderless square buttons, the current page on a gray fill -->
  <nav class="flex flex-wrap items-center gap-x-1" aria-label="Страницы">
    <button
      type="button"
      class="inline-flex min-h-9.5 min-w-9.5 cursor-pointer items-center justify-center rounded-lg px-2 text-sm text-ink hover:bg-shift focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50 disabled:pointer-events-none disabled:opacity-50"
      :disabled="disabled || page <= 1"
      aria-label="Предыдущая страница"
      @click="go(page - 1)"
    >
      <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
        <path d="m15 18-6-6 6-6" />
      </svg>
    </button>

    <template v-for="(item, i) in items" :key="i">
      <span v-if="item === 'gap'" class="inline-flex min-w-9.5 justify-center text-faint" aria-hidden="true">…</span>
      <button
        v-else
        type="button"
        class="mono inline-flex min-h-9.5 min-w-9.5 cursor-pointer items-center justify-center rounded-lg px-3 text-sm text-ink focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50 disabled:pointer-events-none disabled:opacity-50"
        :class="item === page ? 'bg-shift font-medium' : 'hover:bg-shift'"
        :disabled="disabled"
        :aria-current="item === page ? 'page' : undefined"
        @click="go(item)"
      >
        {{ item }}
      </button>
    </template>

    <button
      type="button"
      class="inline-flex min-h-9.5 min-w-9.5 cursor-pointer items-center justify-center rounded-lg px-2 text-sm text-ink hover:bg-shift focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50 disabled:pointer-events-none disabled:opacity-50"
      :disabled="disabled || page >= totalPages"
      aria-label="Следующая страница"
      @click="go(page + 1)"
    >
      <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
        <path d="m9 18 6-6-6-6" />
      </svg>
    </button>
  </nav>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    /** 1-based */
    page: number;
    totalPages: number;
    disabled?: boolean;
  }>(),
  { disabled: false },
);

const emit = defineEmits<{
  "update:page": [page: number];
}>();

/** First, last and the pages around the current one; the rest collapse into gaps */
const items = computed<(number | "gap")[]>(() => {
  const { page, totalPages } = props;
  const shown = [...new Set([1, page - 1, page, page + 1, totalPages])]
    .filter((p) => p >= 1 && p <= totalPages)
    .sort((a, b) => a - b);

  const result: (number | "gap")[] = [];
  for (const p of shown) {
    const prev = result.at(-1);
    // a gap that would hide a single page shows that page instead
    if (typeof prev === "number" && p - prev === 2) result.push(prev + 1);
    else if (typeof prev === "number" && p - prev > 2) result.push("gap");
    result.push(p);
  }
  return result;
});

function go(target: number): void {
  if (target >= 1 && target <= props.totalPages && target !== props.page) emit("update:page", target);
}
</script>
