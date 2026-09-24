<template>
  <!-- Preline stats grid: a row of stat cards -->
  <div class="grid gap-4 sm:grid-cols-2 sm:gap-6 lg:grid-cols-3 2xl:grid-cols-6">
    <UiStat v-for="s in stats" :key="s.label" :value="s.value" :label="s.label" :color="s.color" />
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiStat from "@/components/ui/UiStat.vue";
import { useBoardStore } from "../stores/board";

const board = useBoardStore();

/** colour only when the number needs attention */
const alert = (n: number, color: string) => (n ? color : undefined);

const stats = computed(() => {
  const visits = board.visits;
  const busyCrews = board.crews.filter((c) => c.requests.length).length;
  const urgent = visits.filter((v) => v.urgent).length;
  const outOfWindow = visits.filter((v) => v.outOfWindow).length;
  const unassigned = board.unassigned.length;
  // requests of the EMERGENCY job type left out, as in the «Не вошли в расписание» panel
  const unassignedEmergencies = board.unassigned.filter((r) => r.jobType.keyword === "EMERGENCY").length;
  return [
    { value: visits.length, label: "заявок в расписании" },
    { value: `${busyCrews} / ${board.crews.length}`, label: "бригад задействовано" },
    { value: urgent, label: "со сроком сегодня", color: alert(urgent, "var(--urgent)") },
    { value: outOfWindow, label: "вне запрошенного окна", color: alert(outOfWindow, "var(--unassigned)") },
    { value: unassigned, label: "не вошли в расписание", color: alert(unassigned, "var(--unassigned)") },
    {
      value: unassignedEmergencies,
      label: "аварийных не вошли в расписание",
      color: alert(unassignedEmergencies, "var(--job-emergency)"),
    },
  ];
});
</script>
