<template>
  <UiPanel :title="crew ? crew.name : 'Бригада'">
    <template v-if="crew" #aside>
      <RouterLink
        :to="{ name: 'crew', params: { id: crew.id } }"
        class="rounded-sm font-medium text-accent no-underline hover:underline focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
      >
        Бригада <span class="mono">№{{ crew.id }}</span>
      </RouterLink>
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
import { formatTime } from "../model/format";
import type { CrewDetails } from "../types";

// when the crew was assigned is shown in the request's lifecycle, not here
const props = defineProps<{
  crew: CrewDetails | null;
}>();

const rows = computed<UiDetailRow[]>(() => {
  const crew = props.crew;
  if (!crew) return [];
  return [
    { label: "Смена", value: `${formatTime(crew.shiftStartTime)}–${formatTime(crew.shiftEndTime)}`, mono: true },
    { label: "Транспорт", value: crew.vehicle?.displayName ?? null },
    { label: "Навыки", value: crew.skills.length ? crew.skills.map((skill) => skill.displayName).join(", ") : null },
  ];
});
</script>
