<template>
  <!-- Preline description list: muted labels, divided rows -->
  <dl class="m-0 divide-y divide-line px-5">
    <div v-for="row in rows" :key="row.label" class="grid grid-cols-[minmax(120px,40%)_1fr] gap-x-4 py-3 text-sm">
      <dt class="text-muted">{{ row.label }}</dt>
      <dd class="m-0 min-w-0 break-words" :class="row.value == null ? 'text-faint' : ['text-ink', { mono: row.mono }]">
        {{ row.value ?? "—" }}
      </dd>
    </div>
  </dl>
</template>

<script lang="ts">
export interface UiDetailRow {
  label: string;
  /** null renders a dash */
  value: string | null;
  /** numbers, dates and times */
  mono?: boolean;
}
</script>

<script setup lang="ts">
defineProps<{
  rows: UiDetailRow[];
}>();
</script>
