<template>
  <section class="rounded-lg border border-line bg-surface">
    <div
      class="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-2 border-b border-line px-4 pt-3.5 pb-2.5"
    >
      <h2 class="m-0 text-[15px] font-semibold text-balance">
        <slot name="title">{{ title }}</slot>
      </h2>
      <div v-if="$slots.aside" class="text-[12px] text-muted">
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
