<template>
  <div class="grid">
    <b class="mono text-[20px] font-medium" :style="{ color }">{{ value }}</b>
    <span class="text-[12px] text-muted">{{ label }}</span>
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
