<template>
  <!-- starts edit mode; only the current, calculated schedule can be edited. Hidden while editing: the edit
       bar below the board's controls takes over -->
  <UiButton v-if="!edit.active" variant="solid" :disabled="!canEdit" :title="editDisabledReason ?? undefined" @click="startEditing">
    <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M12 20h9M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
    </svg>
    Редактировать
  </UiButton>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiButton from "@/components/ui/UiButton.vue";
import { useBoardStore } from "../stores/board";
import { useScheduleStore } from "../stores/schedule";
import { useScheduleEditStore } from "../stores/scheduleEdit";

const board = useBoardStore();
const scheduleStore = useScheduleStore();
const edit = useScheduleEditStore();

/** why the schedule can't be edited now; null when it can */
const editDisabledReason = computed(() => {
  if (scheduleStore.viewingHistory) return "Предыдущую версию расписания изменить нельзя";
  const status = scheduleStore.schedule?.status.keyword;
  if (status === "OUTDATED") return "Расписание устарело — изменить его нельзя";
  if (status !== "CREATED") return "Расписание ещё формируется";
  return null;
});
const canEdit = computed(() => editDisabledReason.value === null);

function startEditing(): void {
  const schedule = scheduleStore.schedule;
  if (schedule && canEdit.value) edit.start(schedule.region.id, board.crews);
}
</script>
