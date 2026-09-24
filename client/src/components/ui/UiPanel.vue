<template>
  <!-- console panel: surface fill, hairline border, tight radius -->
  <section class="flex min-w-0 flex-col rounded-lg border border-line bg-surface shadow-2xs">
    <div class="flex flex-wrap items-center justify-between gap-x-4 gap-y-2 border-b border-line px-5 py-4">
      <h2 class="m-0 text-base font-semibold text-balance text-ink">
        <slot name="title">{{ title }}</slot>
      </h2>
      <div v-if="$slots.aside" class="text-sm text-muted">
        <slot name="aside" />
      </div>
    </div>
    <slot />
  </section>
</template>

<script setup lang="ts">
defineProps<{
  title?: string;
}>();
</script>
