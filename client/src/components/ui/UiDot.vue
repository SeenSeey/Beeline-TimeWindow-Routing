<template>
  <span
    class="inline-block flex-none rounded-full"
    :class="[size === 'sm' ? 'size-1.5' : 'size-2', { 'motion-safe:animate-pulse': pulse }]"
    :style="hollow ? { border: `2px solid ${color}` } : { background: color }"
    aria-hidden="true"
  ></span>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    /** any CSS colour; defaults to the text colour */
    color?: string;
    size?: "sm" | "md";
    hollow?: boolean;
    pulse?: boolean;
  }>(),
  { color: "currentColor", size: "md", hollow: false, pulse: false },
);
</script>
