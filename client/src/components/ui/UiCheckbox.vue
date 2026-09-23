<template>
  <label class="inline-flex cursor-pointer items-center gap-1.5 text-muted" :for="id">
    <input :id="id" v-model="checked" type="checkbox" />
    <slot />
  </label>
</template>

<script setup lang="ts">
import { useId } from "vue";

const checked = defineModel<boolean>({ required: true });
const id = useId();
</script>
