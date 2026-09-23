<template>
  <header class="grid gap-2.5">
    <div class="text-[11px] font-medium tracking-[0.08em] text-muted uppercase">
      Расписание на {{ formatDate(schedule.targetDate) }}
    </div>

    <div class="flex flex-wrap items-center gap-x-3 gap-y-1">
      <h1 class="m-0 text-[22px] font-semibold tracking-[-0.01em] text-balance">Доска выездов</h1>
      <ScheduleStatusBadge v-if="schedule.status" :status="schedule.status" />
    </div>

    <div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-muted">
      <slot name="region">
        <span>Регион {{ schedule.region.displayName }}</span>
      </slot>
      <span>офис {{ schedule.region.officeAddress }}</span>
      <span v-if="schedule.createdAt">Расчёт <span class="mono">{{ formatDateTime(schedule.createdAt) }}</span></span>
    </div>

    <BoardStats />
  </header>
</template>

<script setup lang="ts">
import { formatDate, formatDateTime } from "../model/format";
import type { ScheduleData } from "../types";
import BoardStats from "./BoardStats.vue";
import ScheduleStatusBadge from "./ScheduleStatusBadge.vue";

defineProps<{
  schedule: ScheduleData;
}>();
</script>
