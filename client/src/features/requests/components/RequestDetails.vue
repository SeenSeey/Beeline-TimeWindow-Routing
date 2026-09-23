<template>
  <!-- same page frame as the schedule board (timeline BoardLayout) -->
  <div class="px-5 pt-5 pb-10">
    <div class="mx-auto grid max-w-[1440px] grid-cols-[minmax(0,1fr)] gap-5">
      <header class="grid gap-2">
        <RouterLink
          :to="{ name: 'requests' }"
          class="w-fit text-[13px] text-muted no-underline hover:text-ink focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
        >
          ← Все заявки
        </RouterLink>

        <div class="flex flex-wrap items-center gap-x-3 gap-y-2">
          <h1 class="m-0 text-[22px] font-semibold tracking-[-0.01em] text-balance">
            Заявка <span class="mono">№{{ request.id }}</span>
          </h1>
          <UiBadge v-if="request.status" size="md" :color="statusColor(request.status.keyword)" dot>
            {{ request.status.displayName }}
          </UiBadge>
          <UiBadge size="md" :color="jobTypeColor(request.jobType.keyword)">{{ request.jobType.displayName }}</UiBadge>
          <UiBadge v-if="request.urgent" size="md" color="var(--urgent)">Срочная</UiBadge>
        </div>

        <p class="m-0 text-[15px]">{{ request.fullAddress }}</p>
      </header>

      <div class="grid grid-cols-1 items-start gap-5 min-[901px]:grid-cols-2">
        <UiPanel title="Место">
          <UiDetailList :rows="placeRows" />
        </UiPanel>

        <UiPanel title="Сроки">
          <UiDetailList :rows="timeRows" />
        </UiPanel>

        <UiPanel title="Работы и требования">
          <UiDetailList :rows="requirementRows" />
        </UiPanel>

        <RequestCrewPanel :crew="request.assignedCrew" :assigned-at="request.assignedAt" />
      </div>

      <RequestVisitsPanel :visits="request.scheduledVisits" :region-id="request.region.id" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiBadge from "@/components/ui/UiBadge.vue";
import UiDetailList, { type UiDetailRow } from "@/components/ui/UiDetailList.vue";
import UiPanel from "@/components/ui/UiPanel.vue";
import { jobTypeColor, statusColor } from "../model/colors";
import { formatDateTime, formatDuration, formatOptionalDateTime, formatTime, mapUrl } from "../model/format";
import type { RequestDetailed } from "../types";
import RequestCrewPanel from "./RequestCrewPanel.vue";
import RequestVisitsPanel from "./RequestVisitsPanel.vue";

const props = defineProps<{
  request: RequestDetailed;
}>();

const joinOrNull = (items: string[]): string | null => (items.length ? items.join(", ") : null);

const placeRows = computed<UiDetailRow[]>(() => {
  const r = props.request;
  return [
    { label: "Адрес", value: r.fullAddress },
    { label: "Регион", value: r.region.displayName },
    { label: "Офис региона", value: r.region.officeAddress },
    { label: "Координаты", value: `${r.latitude.toFixed(5)}, ${r.longitude.toFixed(5)}`, mono: true },
  ];
});

const timeRows = computed<UiDetailRow[]>(() => {
  const r = props.request;
  return [
    {
      label: "Окно визита",
      value: `${formatTime(r.requestTimeWindowStart)}–${formatTime(r.requestTimeWindowEnd)}`,
      mono: true,
    },
    { label: "Крайний срок", value: formatDateTime(r.deadline), mono: true },
    { label: "Создана", value: formatDateTime(r.createdAt), mono: true },
    { label: "Назначена", value: formatOptionalDateTime(r.assignedAt), mono: true },
    { label: "Выполнена", value: formatOptionalDateTime(r.completedAt), mono: true },
    { label: "Отменена", value: formatOptionalDateTime(r.canceledAt), mono: true },
  ];
});

const requirementRows = computed<UiDetailRow[]>(() => {
  const r = props.request;
  const j = r.jobType;
  return [
    { label: "Тип работ", value: j.displayName },
    { label: "Работа", value: formatDuration(j.jobTimeSpanMinutes) },
    { label: "Дорога", value: `до ${formatDuration(j.maxTravelTimeSpanMinutes)}` },
    { label: "Всего", value: formatDuration(j.totalTimeSpanMinutes) },
    { label: "Навыки", value: joinOrNull(j.requiredSkills.map((s) => s.displayName)) },
    { label: "Оборудование", value: joinOrNull(r.equipment) },
    { label: "Транспорт", value: r.requiredVehicle?.displayName ?? null },
  ];
});
</script>
