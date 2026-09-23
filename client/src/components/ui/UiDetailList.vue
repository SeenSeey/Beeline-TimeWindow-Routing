<template>
  <dl class="m-0 grid grid-cols-[minmax(110px,auto)_1fr] gap-x-4 gap-y-2 px-4 py-3.5">
    <template v-for="row in rows" :key="row.label">
      <dt class="text-muted">{{ row.label }}</dt>
      <dd class="m-0 min-w-0 break-words" :class="{ mono: row.mono, 'text-faint': row.value == null }">
        {{ row.value ?? "—" }}
      </dd>
    </template>
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
