<template>
  <span
    class="inline-flex items-center gap-1.5 rounded-full border border-current whitespace-nowrap"
    :class="size === 'md' ? 'px-2.5 py-0.5 text-[12px] font-medium' : 'px-[7px] py-px text-[11px]'"
    :style="{ color }"
  >
    <UiDot v-if="dot" />
    <slot />
  </span>
</template>

<script setup lang="ts">
import UiDot from "./UiDot.vue";

withDefaults(
  defineProps<{
    /** any CSS colour; used for text and border */
    color?: string;
    size?: "sm" | "md";
    dot?: boolean;
  }>(),
  { color: "var(--muted)", size: "sm", dot: false },
);
</script>
