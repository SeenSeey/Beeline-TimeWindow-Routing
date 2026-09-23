<template>
  <nav class="flex flex-wrap items-center gap-1" aria-label="Страницы">
    <UiButton size="icon" :disabled="disabled || page <= 1" aria-label="Предыдущая страница" @click="go(page - 1)">
      <svg class="h-2.5 w-2.5" viewBox="0 0 10 10" aria-hidden="true">
        <path d="M6.5 1.5 3 5l3.5 3.5" fill="none" stroke="currentColor" stroke-width="1.5" />
      </svg>
    </UiButton>

    <template v-for="(item, i) in items" :key="i">
      <span v-if="item === 'gap'" class="w-6 text-center text-faint" aria-hidden="true">…</span>
      <UiButton
        v-else
        size="icon"
        class="mono"
        :variant="item === page ? 'solid' : 'outline'"
        :disabled="disabled"
        :aria-current="item === page ? 'page' : undefined"
        @click="go(item)"
      >
        {{ item }}
      </UiButton>
    </template>

    <UiButton
      size="icon"
      :disabled="disabled || page >= totalPages"
      aria-label="Следующая страница"
      @click="go(page + 1)"
    >
      <svg class="h-2.5 w-2.5" viewBox="0 0 10 10" aria-hidden="true">
        <path d="M3.5 1.5 7 5 3.5 8.5" fill="none" stroke="currentColor" stroke-width="1.5" />
      </svg>
    </UiButton>
  </nav>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiButton from "./UiButton.vue";

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
