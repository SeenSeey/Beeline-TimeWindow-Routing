<template>
  <!-- Preline soft badge: tinted background in the badge colour, coloured text -->
  <span
    class="inline-flex items-center gap-x-1.5 rounded-full bg-current/12 text-xs font-medium whitespace-nowrap"
    :class="size === 'md' ? 'px-2.5 py-1' : 'px-2 py-0.5'"
    :style="{ color }"
  >
    <UiDot v-if="dot" size="sm" />
    <slot />
  </span>
</template>

<script setup lang="ts">
import UiDot from "./UiDot.vue";

withDefaults(
  defineProps<{
    /** any CSS colour; used for the text and, tinted, for the background */
    color?: string;
    size?: "sm" | "md";
    dot?: boolean;
  }>(),
  { color: "var(--muted)", size: "sm", dot: false },
);
</script>
