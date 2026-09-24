<template>
  <header class="grid gap-4 sm:gap-6">
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div class="grid gap-1">
        <p class="m-0 text-sm text-muted">Расписание на {{ formatDate(schedule.targetDate) }}</p>

        <div class="flex flex-wrap items-center gap-x-3 gap-y-1">
          <h1 class="m-0 text-2xl font-semibold text-balance text-ink">Доска выездов</h1>
          <ScheduleStatusBadge v-if="schedule.status" :status="schedule.status" />
        </div>

        <div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-muted">
          <span>офис {{ schedule.region.officeAddress }}</span>
          <span v-if="schedule.createdAt">Расчёт <span class="mono">{{ formatDateTime(schedule.createdAt) }}</span></span>
        </div>
      </div>

      <slot name="region">
        <span class="text-sm text-muted">Регион {{ schedule.region.displayName }}</span>
      </slot>
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
