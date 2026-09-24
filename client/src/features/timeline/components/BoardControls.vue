<template>
  <!-- Preline toolbar card -->
  <div class="flex flex-wrap items-center gap-x-4 gap-y-3 rounded-lg border border-line bg-surface p-4 shadow-2xs">
    <JobTypeFilter />
    <UiCheckbox v-model="board.onlyUrgent">только бригады с заявками со сроком сегодня</UiCheckbox>
    <ZoomControls class="ms-auto" />
  </div>
</template>

<script setup lang="ts">
import UiCheckbox from "@/components/ui/UiCheckbox.vue";
import { useBoardStore } from "../stores/board";
import JobTypeFilter from "./JobTypeFilter.vue";
import ZoomControls from "./ZoomControls.vue";

const board = useBoardStore();
</script>
