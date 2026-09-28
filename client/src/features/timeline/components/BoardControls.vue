<template>
  <!-- Preline toolbar card, one row (wrapping on narrow screens): the version shown (the page's switch, in the
       slot), what the board shows (filters), then zoom and editing -->
  <div class="flex flex-wrap items-center gap-x-4 gap-y-2.5 rounded-lg border border-line bg-surface px-4 py-2.5 shadow-2xs">
    <slot name="versions" />
    <JobTypeFilter />
    <UiCheckbox v-model="board.onlyUrgent">срок выполнения — сегодня</UiCheckbox>
    <div class="ms-auto flex flex-wrap items-center gap-3">
      <ZoomControls />
      <ScheduleEditButton />
    </div>
  </div>
</template>

<script setup lang="ts">
import UiCheckbox from "@/components/ui/UiCheckbox.vue";
import { useBoardStore } from "../stores/board";
import JobTypeFilter from "./JobTypeFilter.vue";
import ScheduleEditButton from "./ScheduleEditButton.vue";
import ZoomControls from "./ZoomControls.vue";

const board = useBoardStore();
</script>
