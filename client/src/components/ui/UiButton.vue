<template>
  <!-- Preline buttons: white (outline) with a hairline shadow, or solid blue -->
  <button
    :type="type"
    class="inline-flex cursor-pointer items-center justify-center gap-x-2 rounded-lg border text-sm font-medium whitespace-nowrap focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50 disabled:pointer-events-none disabled:opacity-50 motion-safe:transition-colors"
    :class="[
      filled
        ? 'border-transparent bg-accent text-white hover:bg-accent-hover'
        : 'border-line bg-surface text-ink shadow-2xs hover:bg-shift',
      size === 'icon' ? 'size-9.5 px-0' : 'h-9.5 px-3',
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
