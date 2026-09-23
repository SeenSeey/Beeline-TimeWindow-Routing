<template>
  <li class="grid grid-cols-[14px_1fr_auto] items-baseline gap-x-2.5 border-t border-line px-4 py-2.5 first:border-t-0">
    <UiDot :color="color" />

    <div class="grid min-w-0 gap-1">
      <RouterLink
        :to="{ name: 'request', params: { id: request.id } }"
        class="block truncate text-ink no-underline hover:text-accent hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
        :title="request.fullAddress"
      >
        {{ request.fullAddress }}
      </RouterLink>

      <div class="flex flex-wrap gap-1.5">
        <UiBadge :color="color">{{ request.jobType.displayName }}</UiBadge>
        <UiBadge v-if="request.urgent" color="var(--unassigned)">срочная</UiBadge>
        <UiBadge v-if="dueToday" color="var(--urgent)">срок сегодня</UiBadge>
        <UiBadge v-if="request.status">{{ request.status.displayName }}</UiBadge>
      </div>

      <dl class="m-0 grid grid-cols-[auto_1fr] gap-x-3 gap-y-0.5 text-[12px]">
        <template v-for="d in details" :key="d.label">
          <dt class="text-muted">{{ d.label }}</dt>
          <dd class="m-0" :class="{ mono: d.mono }">{{ d.value }}</dd>
        </template>
      </dl>
    </div>

    <div class="mono text-right text-[12px] whitespace-nowrap">
      №{{ request.id }}
      <div class="text-muted">создана {{ formatDateTime(request.createdAt) }}</div>
    </div>
  </li>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiBadge from "@/components/ui/UiBadge.vue";
import UiDot from "@/components/ui/UiDot.vue";
import { jobTypeColor } from "../model/colors";
import { formatDateTime, formatTime, isOnDate } from "../model/format";
import type { UnassignedRequest } from "../types";

const props = defineProps<{
  request: UnassignedRequest;
  /** schedule's "YYYY-MM-DD", to tell deadlines due today */
  targetDate: string;
}>();

const color = computed(() => jobTypeColor(props.request.jobType.keyword));
const dueToday = computed(() => isOnDate(props.request.deadline, props.targetDate));

/** Label/value rows; optional ones only when the server sent something */
const details = computed(() => {
  const r = props.request;
  const rows: { label: string; value: string; mono?: boolean }[] = [
    {
      label: "Окно",
      value: `${formatTime(r.requestTimeWindowStart)}–${formatTime(r.requestTimeWindowEnd)}`,
      mono: true,
    },
    {
      label: "Срок",
      value: dueToday.value ? `сегодня ${r.deadline.slice(11, 16)}` : formatDateTime(r.deadline),
      mono: true,
    },
    { label: "Работа", value: `${r.jobTimeSpanMinutes} мин · дорога до ${r.maxTravelTimeSpanMinutes} мин` },
  ];
  if (r.requiredSkills.length) {
    rows.push({ label: "Навыки", value: r.requiredSkills.map((s) => s.displayName).join(", ") });
  }
  if (r.equipment.length) rows.push({ label: "Оборудование", value: r.equipment.join(", ") });
  if (r.requiredVehicle) rows.push({ label: "Транспорт", value: r.requiredVehicle.displayName });
  if (r.assignedCrew) {
    const at = r.assignedAt ? ` · назначена ${formatDateTime(r.assignedAt)}` : "";
    rows.push({ label: "Бригада", value: r.assignedCrew + at });
  }
  rows.push({ label: "Координаты", value: `${r.latitude.toFixed(5)}, ${r.longitude.toFixed(5)}`, mono: true });
  return rows;
});
</script>
