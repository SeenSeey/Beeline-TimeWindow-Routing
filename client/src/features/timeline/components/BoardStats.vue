<template>
  <!-- the day's figures as one thin strip, number then label, divided; a number that needs attention is
       coloured. Wraps on narrow screens instead of growing into cards -->
  <dl class="m-0 flex flex-wrap items-stretch rounded-lg border border-line bg-surface shadow-2xs">
    <div
      v-for="s in stats"
      :key="s.label"
      class="flex items-baseline gap-x-1.5 border-line px-4 py-2 [&:not(:last-child)]:border-e"
    >
      <!-- dt before dd, as a definition list wants; the number is shown first (order-first) -->
      <dt class="text-xs whitespace-nowrap text-muted">{{ s.label }}</dt>
      <dd class="mono order-first m-0 text-base font-semibold text-ink" :style="s.color ? { color: s.color } : undefined">
        {{ s.value }}
      </dd>
    </div>
  </dl>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { formatDistance } from "../model/format";
import { useBoardStore } from "../stores/board";
import { useScheduleStore } from "../stores/schedule";

const board = useBoardStore();
const scheduleStore = useScheduleStore();

/** colour only when the number needs attention */
const alert = (n: number, color: string) => (n ? color : undefined);

/**
 * "123,4 км" — the region's crews' stored routes together; "—" while there are none (the schedule is
 * calculating). Taken from the saved schedule: in edit mode the edited crews' preview rows have no route.
 */
const totalRouteDistance = computed(() => {
  const distances = (scheduleStore.schedule?.engineers ?? []).flatMap((crew) =>
    crew.routeDistanceKilometers != null ? [crew.routeDistanceKilometers] : [],
  );
  if (!distances.length) return "—";
  const kilometers = distances.reduce((sum, distance) => sum + distance, 0);
  return formatDistance(kilometers);
});

const stats = computed(() => {
  const visits = board.visits;
  const busyCrews = board.crews.filter((c) => c.requests.length).length;
  const urgent = visits.filter((v) => v.urgent).length;
  const outOfWindow = visits.filter((v) => v.outOfWindow).length;
  const unassigned = board.unassigned.length;
  // requests of the EMERGENCY job type left out, as in the «Не вошли в расписание» panel
  const unassignedEmergencies = board.unassigned.filter((r) => r.jobType.keyword === "EMERGENCY").length;
  const all = [
    { value: visits.length, label: "заявок в расписании" },
    { value: `${busyCrews} / ${board.crews.length}`, label: "бригад задействовано" },
    { value: totalRouteDistance.value, label: "общий пробег" },
    { value: urgent, label: "со сроком сегодня", color: alert(urgent, "var(--urgent)") },
    // only when some visit starts outside its window: a zero here says nothing worth the room
    { value: outOfWindow, label: "вне окна", color: alert(outOfWindow, "var(--unassigned)"), hideWhenZero: true },
    { value: unassigned, label: "не вошли в расписание", color: alert(unassigned, "var(--unassigned)") },
    {
      value: unassignedEmergencies,
      label: "из них аварийных",
      color: alert(unassignedEmergencies, "var(--job-emergency)"),
    },
  ];
  return all.filter((stat) => !("hideWhenZero" in stat && stat.hideWhenZero && stat.value === 0));
});
</script>
