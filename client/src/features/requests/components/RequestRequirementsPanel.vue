<template>
  <UiPanel title="Работы и требования">
    <template #aside>{{ jobType.displayName }}</template>

    <!-- time budget: three figures side by side -->
    <dl class="m-0 grid grid-cols-3 divide-x divide-line border-b border-line">
      <div v-for="figure in timeFigures" :key="figure.label" class="grid gap-0.5 px-5 py-3">
        <dt class="text-xs text-muted">{{ figure.label }}</dt>
        <dd class="mono m-0 text-sm font-semibold text-ink">{{ figure.value }}</dd>
      </div>
    </dl>

    <dl class="m-0 divide-y divide-line px-5">
      <div v-for="group in groups" :key="group.label" class="grid grid-cols-[minmax(120px,30%)_1fr] gap-x-4 py-3 text-sm">
        <dt class="text-muted">{{ group.label }}</dt>
        <dd class="m-0 flex min-w-0 flex-wrap gap-1.5">
          <template v-if="group.items.length">
            <UiBadge v-for="item in group.items" :key="item">{{ item }}</UiBadge>
          </template>
          <span v-else class="text-faint">—</span>
        </dd>
      </div>
    </dl>
  </UiPanel>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiBadge from "@/components/ui/UiBadge.vue";
import UiPanel from "@/components/ui/UiPanel.vue";
import { formatDuration } from "../model/format";
import type { RequestDetailed } from "../types";

const props = defineProps<{
  request: RequestDetailed;
}>();

const jobType = computed(() => props.request.jobType);

const timeFigures = computed(() => [
  { label: "Работа", value: formatDuration(jobType.value.jobTimeSpanMinutes) },
  { label: "Дорога", value: `до ${formatDuration(jobType.value.maxTravelTimeSpanMinutes)}` },
  { label: "Всего", value: formatDuration(jobType.value.totalTimeSpanMinutes) },
]);

/** what the crew must bring: each as a row of badges */
const groups = computed(() => [
  { label: "Навыки", items: jobType.value.requiredSkills.map((skill) => skill.displayName) },
  { label: "Оборудование", items: props.request.equipment },
  { label: "Транспорт", items: props.request.requiredVehicle ? [props.request.requiredVehicle.displayName] : [] },
]);
</script>
