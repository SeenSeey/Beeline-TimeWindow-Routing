<template>
  <!-- A sortable table column head: the label as a button, with the order it sorts by. Clicks go
       ascending → descending → unsorted; the table decides what that means -->
  <th scope="col" :aria-sort="ariaSort">
    <button
      type="button"
      class="group inline-flex cursor-pointer items-center gap-x-1 rounded-sm uppercase focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
      :class="direction ? 'text-accent' : 'hover:text-accent'"
      :title="title"
      @click="emit('toggle')"
    >
      <slot />
      <svg
        class="size-3.5 shrink-0"
        :class="direction ? '' : 'opacity-30 group-hover:opacity-70'"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2.5"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <path v-if="direction === 'asc'" d="m6 15 6-6 6 6" />
        <path v-else-if="direction === 'desc'" d="m6 9 6 6 6-6" />
        <path v-else d="m7 15 5 5 5-5M7 9l5-5 5 5" />
      </svg>
    </button>
  </th>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  /** the order the column sorts by now; null when the table isn't sorted by it */
  direction: "asc" | "desc" | null;
}>();

const emit = defineEmits<{
  toggle: [];
}>();

const ariaSort = computed(() =>
  props.direction === "asc" ? "ascending" : props.direction === "desc" ? "descending" : "none",
);

const title = computed(() =>
  props.direction === "asc"
    ? "Сортировать по убыванию"
    : props.direction === "desc"
      ? "Убрать сортировку"
      : "Сортировать по возрастанию",
);
</script>
