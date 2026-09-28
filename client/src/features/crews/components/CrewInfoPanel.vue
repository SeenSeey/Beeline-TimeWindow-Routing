<template>
  <!-- the crew itself in one panel, like the request's: the key figures, then what it can do and has on hand -->
  <UiPanel title="Сведения о бригаде">
    <!-- the figures a dispatcher checks first, side by side -->
    <dl class="m-0 grid grid-cols-2 border-b border-line sm:grid-cols-4">
      <div
        v-for="(fact, index) in facts"
        :key="fact.label"
        class="grid content-start gap-0.5 border-line px-5 py-3"
        :class="{ 'sm:border-s': index > 0, 'max-sm:border-s': index % 2 === 1, 'max-sm:border-t': index > 1 }"
      >
        <dt class="text-xs text-muted">{{ fact.label }}</dt>
        <dd class="m-0 text-sm font-semibold" :class="[fact.value == null ? 'text-faint' : 'text-ink', { mono: fact.mono }]">
          {{ fact.value ?? "—" }}
        </dd>
        <dd v-if="fact.hint" class="m-0 text-xs text-muted">{{ fact.hint }}</dd>
      </div>
    </dl>

    <dl class="m-0 divide-y divide-line px-5">
      <div v-for="group in capabilityGroups" :key="group.label" class="grid grid-cols-[minmax(110px,28%)_1fr] gap-x-4 py-3 text-sm">
        <dt class="text-muted">{{ group.label }}</dt>
        <dd class="m-0 flex min-w-0 flex-wrap gap-1.5">
          <template v-if="group.items.length">
            <UiBadge v-for="item in group.items" :key="item.label" :color="item.color">{{ item.label }}</UiBadge>
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
import { formatTime } from "@/features/timeline/model/format";
import type { CrewDetailed, SkillKeyword } from "../types";

const props = defineProps<{
  crew: CrewDetailed;
}>();

const facts = computed(() => {
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
    { label: "Заявок на сегодня", value: String(crew.route.length), hint: `активных назначено: ${activeCount}`, mono: true },
  ];
});

/** each skill in the colour of the job types it covers (as in the crews table) */
const SKILL_COLORS: Record<SkillKeyword, string> = {
  LOCAL_WORK: "var(--job-local)",
  INSTALLATION_OR_ADDON: "var(--job-installation)",
  EMERGENCY: "var(--job-emergency)",
};

/** what the crew can do and what it has on hand now (issued with the schedule, minus requests taken into work) */
const capabilityGroups = computed(() => [
  {
    label: "Навыки",
    items: props.crew.skills.map((skill) => ({
      label: skill.displayName,
      color: SKILL_COLORS[skill.keyword] ?? "var(--muted)",
    })),
  },
  {
    label: "Оборудование",
    items: props.crew.equipment.map((item) => ({ label: `${item.displayName} × ${item.quantity}`, color: "var(--muted)" })),
  },
]);
</script>
