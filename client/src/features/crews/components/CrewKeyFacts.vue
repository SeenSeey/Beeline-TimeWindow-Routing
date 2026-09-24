<template>
  <!-- the crew at a glance, as tiles (same look as the request details' key facts) -->
  <dl class="m-0 grid gap-4 sm:grid-cols-2 sm:gap-6 xl:grid-cols-4">
    <div
      v-for="fact in facts"
      :key="fact.label"
      class="flex min-w-0 flex-col gap-y-1 rounded-lg border border-line bg-surface p-4 shadow-2xs"
    >
      <dt class="text-xs text-muted">{{ fact.label }}</dt>
      <dd
        class="m-0 truncate text-base font-semibold"
        :class="[fact.value == null ? 'text-faint' : 'text-ink', { mono: fact.mono }]"
        :title="fact.value ?? undefined"
      >
        {{ fact.value ?? "—" }}
      </dd>
      <dd v-if="fact.hint" class="m-0 truncate text-xs text-muted">{{ fact.hint }}</dd>
    </div>
  </dl>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { formatTime } from "@/features/timeline/model/format";
import type { CrewDetailed } from "../types";

const props = defineProps<{
  crew: CrewDetailed;
}>();

interface KeyFact {
  label: string;
  value: string | null;
  /** second, smaller line */
  hint?: string;
  mono?: boolean;
}

const facts = computed<KeyFact[]>(() => {
  const crew = props.crew;
  const activeCount = crew.assignedRequests.filter((request) => !request.completedAt && !request.canceledAt).length;
  return [
    { label: "Регион", value: crew.region.displayName, hint: `офис ${crew.region.officeAddress}` },
    { label: "Смена", value: `${formatTime(crew.shiftStartTime)}–${formatTime(crew.shiftEndTime)}`, mono: true },
    {
      label: "Транспорт",
      value: crew.vehicle?.displayName ?? null,
      hint: crew.vehicle ? `в среднем ${crew.vehicle.averageSpeedKmh} км/ч` : undefined,
    },
    {
      label: "Заявок на сегодня",
      value: String(crew.route.length),
      hint: `активных назначено: ${activeCount}`,
      mono: true,
    },
  ];
});
</script>
