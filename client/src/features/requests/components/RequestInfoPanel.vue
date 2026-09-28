<template>
  <!-- the request itself in one panel: the key figures and what the work needs (the crew has its own card) -->
  <UiPanel title="Сведения о заявке">
    <!-- the figures a dispatcher checks first, side by side -->
    <dl class="m-0 grid grid-cols-2 border-b border-line sm:grid-cols-4">
      <div
        v-for="(fact, index) in facts"
        :key="fact.label"
        class="grid content-start gap-0.5 border-line px-5 py-3"
        :class="{ 'sm:border-s': index > 0, 'max-sm:border-s': index % 2 === 1, 'max-sm:border-t': index > 1 }"
      >
        <dt class="text-xs text-muted">{{ fact.label }}</dt>
        <dd class="m-0 text-sm font-semibold text-ink" :class="{ mono: fact.mono }">{{ fact.value }}</dd>
        <dd v-if="fact.hint" class="m-0 text-xs text-muted">{{ fact.hint }}</dd>
      </div>
    </dl>

    <section aria-labelledby="request-requirements">
      <dl class="m-0 divide-y divide-line px-5">
        <div class="grid grid-cols-[minmax(110px,28%)_1fr] gap-x-4 py-3 text-sm">
          <dt class="text-muted">Время</dt>
          <dd class="mono m-0 text-ink">
            работа {{ formatDuration(jobType.jobTimeSpanMinutes) }} <span class="text-faint">·</span> дорога до
            {{ formatDuration(jobType.maxTravelTimeSpanMinutes) }}
          </dd>
        </div>
        <!-- the job type's time budget, work and travel together -->
        <div class="grid grid-cols-[minmax(110px,28%)_1fr] gap-x-4 py-3 text-sm">
          <dt class="text-muted">Норматив выполнения</dt>
          <dd class="mono m-0 font-medium text-ink">{{ formatDuration(jobType.totalTimeSpanMinutes) }}</dd>
        </div>
        <div
          v-for="group in requirementGroups"
          :key="group.label"
          class="grid grid-cols-[minmax(110px,28%)_1fr] gap-x-4 py-3 text-sm"
        >
          <dt class="text-muted">{{ group.label }}</dt>
          <dd class="m-0 flex min-w-0 flex-wrap gap-1.5">
            <template v-if="group.items.length">
              <UiBadge v-for="item in group.items" :key="item">{{ item }}</UiBadge>
            </template>
            <span v-else class="text-faint">—</span>
          </dd>
        </div>
      </dl>
    </section>
  </UiPanel>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiBadge from "@/components/ui/UiBadge.vue";
import UiPanel from "@/components/ui/UiPanel.vue";
import { formatDateTime, formatDuration, formatTime } from "../model/format";
import type { RequestDetailed } from "../types";

const props = defineProps<{
  request: RequestDetailed;
}>();

const jobType = computed(() => props.request.jobType);

const facts = computed(() => {
  const request = props.request;
  return [
    {
      label: "Окно визита",
      value: `${formatTime(request.requestTimeWindowStart)}–${formatTime(request.requestTimeWindowEnd)}`,
      mono: true,
    },
    { label: "Крайний срок", value: formatDateTime(request.deadline), mono: true },
    { label: "Тип работ", value: jobType.value.displayName },
    { label: "Создана", value: formatDateTime(request.createdAt), mono: true },
  ];
});

/** what the crew must bring: each as a row of badges */
const requirementGroups = computed(() => [
  { label: "Навыки", items: jobType.value.requiredSkills.map((skill) => skill.displayName) },
  { label: "Оборудование", items: props.request.equipment },
  { label: "Транспорт", items: props.request.requiredVehicle ? [props.request.requiredVehicle.displayName] : [] },
]);
</script>
