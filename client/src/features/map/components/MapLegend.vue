<template>
  <ul class="m-0 flex list-none flex-wrap items-center gap-x-4 gap-y-1.5 p-0 text-xs text-muted">
    <li v-for="j in jobTypes" :key="j.keyword" class="inline-flex items-center gap-1.5">
      <UiDot :color="j.color" />{{ j.displayName }}
    </li>
    <li class="inline-flex items-center gap-1.5">
      <span class="inline-block h-[11px] w-[11px] rounded-full border-2 border-urgent" aria-hidden="true"></span>
      срочная
    </li>
    <li class="inline-flex items-center gap-1.5">
      <UiDot color="var(--muted)" class="opacity-35" />выполнена или отменена
    </li>
  </ul>
</template>

<script setup lang="ts">
import UiDot from "@/components/ui/UiDot.vue";
import type { JobTypeOption } from "@/features/timeline/model/colors";

defineProps<{
  /** job types present on the map, with their colours */
  jobTypes: JobTypeOption[];
}>();
</script>
