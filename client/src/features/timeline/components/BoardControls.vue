<template>
  <!-- Preline toolbar card -->
  <div class="flex flex-wrap items-center gap-x-4 gap-y-3 rounded-lg border border-line bg-surface p-4 shadow-2xs">
    <JobTypeFilter />
    <UiCheckbox v-model="board.onlyUrgent">только бригады с заявками со сроком сегодня</UiCheckbox>
    <div class="ms-auto flex flex-wrap items-center gap-3">
      <ZoomControls />
      <!-- edit mode: only a calculated schedule can be edited -->
      <UiButton
        v-if="!edit.active"
        variant="solid"
        :disabled="!canEdit"
        :title="canEdit ? undefined : 'Расписание ещё формируется'"
        @click="startEditing"
      >
        <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M12 20h9M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
        </svg>
        Редактировать
      </UiButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiButton from "@/components/ui/UiButton.vue";
import UiCheckbox from "@/components/ui/UiCheckbox.vue";
import { useBoardStore } from "../stores/board";
import { useScheduleStore } from "../stores/schedule";
import { useScheduleEditStore } from "../stores/scheduleEdit";
import JobTypeFilter from "./JobTypeFilter.vue";
import ZoomControls from "./ZoomControls.vue";

const board = useBoardStore();
const scheduleStore = useScheduleStore();
const edit = useScheduleEditStore();

const canEdit = computed(() => scheduleStore.schedule?.status.keyword === "CREATED");

function startEditing(): void {
  const schedule = scheduleStore.schedule;
  if (schedule) edit.start(schedule.region.id, board.crews);
}
</script>
