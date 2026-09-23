<template>
  <button
    :type="type"
    class="inline-flex cursor-pointer items-center gap-1.5 rounded-full border py-1 text-[13px] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent disabled:cursor-default disabled:opacity-40 motion-safe:transition-colors motion-safe:duration-[120ms]"
    :class="[
      filled ? 'border-ink bg-ink text-canvas' : 'border-line-strong bg-surface text-ink',
      size === 'icon' ? 'w-[30px] justify-center px-0' : 'px-[11px]',
    ]"
    :aria-pressed="pressed"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    variant?: "outline" | "solid";
    /** toggle button: pressed renders like the solid variant */
    pressed?: boolean;
    size?: "md" | "icon";
    type?: "button" | "submit";
  }>(),
  { variant: "outline", pressed: undefined, size: "md", type: "button" },
);

const filled = computed(() => props.variant === "solid" || props.pressed === true);
</script>
