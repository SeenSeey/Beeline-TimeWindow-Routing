<template>
  <UiPanel :title="crew ? crew.name : 'Бригада'">
    <template v-if="crew" #aside>
      <span class="mono">№{{ crew.id }}</span>
    </template>

    <UiEmptyState v-if="!crew">Бригада ещё не назначена.</UiEmptyState>
    <UiDetailList v-else :rows="rows" />
  </UiPanel>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiDetailList, { type UiDetailRow } from "@/components/ui/UiDetailList.vue";
import UiEmptyState from "@/components/ui/UiEmptyState.vue";
import UiPanel from "@/components/ui/UiPanel.vue";
import { formatOptionalDateTime, formatTime } from "../model/format";
import type { CrewDetails } from "../types";

const props = defineProps<{
  crew: CrewDetails | null;
  /** ISO LocalDateTime */
  assignedAt: string | null;
}>();

const rows = computed<UiDetailRow[]>(() => {
  const c = props.crew;
  if (!c) return [];
  return [
    { label: "Назначена", value: formatOptionalDateTime(props.assignedAt), mono: true },
    { label: "Смена", value: `${formatTime(c.shiftStartTime)}–${formatTime(c.shiftEndTime)}`, mono: true },
    { label: "Транспорт", value: c.vehicle?.displayName ?? null },
    { label: "Навыки", value: c.skills.length ? c.skills.map((s) => s.displayName).join(", ") : null },
    { label: "Оборудование", value: c.equipment.length ? c.equipment.join(", ") : null },
  ];
});
</script>
