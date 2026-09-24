<template>
  <!-- stat tile: large value over a small label, with a short accent bar underneath -->
  <div class="flex flex-col gap-y-1.5 rounded-lg border border-line bg-surface p-4 shadow-2xs">
    <b class="mono text-2xl font-semibold text-ink" :style="{ color }">{{ value }}</b>
    <span class="text-xs text-muted">{{ label }}</span>
    <span class="mt-1 h-1 w-10 rounded-full bg-accent" :style="color ? { background: color } : undefined"></span>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  value: string | number;
  label: string;
  /** any CSS colour for the value; inherits when omitted */
  color?: string;
}>();
</script>
