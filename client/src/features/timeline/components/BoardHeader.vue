<template>
  <header class="grid gap-3">
    <div class="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
      <!-- the title (with the status when it's worth noticing), and under it when the schedule was calculated -->
      <div class="grid min-w-0 gap-0.5">
        <div class="flex flex-wrap items-center gap-x-3 gap-y-1">
          <h1 class="m-0 text-xl font-semibold text-ink">Доска выездов</h1>
          <ScheduleStatusBadge
            v-if="schedule.status && schedule.status.keyword !== 'CREATED'"
            :status="schedule.status"
          />
        </div>
        <p v-if="schedule.createdAt" class="m-0 text-sm text-muted">
          от <span class="mono">{{ formatDateTime(schedule.createdAt) }}</span>
        </p>
      </div>

      <slot name="region">
        <span class="text-sm text-muted">Регион {{ schedule.region.displayName }}</span>
      </slot>
    </div>

    <BoardStats />
  </header>
</template>

<script setup lang="ts">
import { formatDateTime } from "../model/format";
import type { ScheduleData } from "../types";
import BoardStats from "./BoardStats.vue";
import ScheduleStatusBadge from "./ScheduleStatusBadge.vue";

defineProps<{
  schedule: ScheduleData;
}>();
</script>
