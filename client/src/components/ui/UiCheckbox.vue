<template>
  <label class="inline-flex cursor-pointer items-center gap-x-2 text-sm text-muted" :for="id">
    <input
      :id="id"
      v-model="checked"
      type="checkbox"
      class="size-4 shrink-0 cursor-pointer rounded-sm border-line-strong accent-accent focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
    />
    <slot />
  </label>
</template>

<script setup lang="ts">
import { useId } from "vue";

const checked = defineModel<boolean>({ required: true });
const id = useId();
</script>
