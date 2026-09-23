<template>
  <UiPanel title="В расписаниях">
    <template #aside>{{ visits.length }}</template>

    <UiEmptyState v-if="!visits.length">Заявка пока не попадала ни в одно расписание.</UiEmptyState>

    <div v-else class="overflow-x-auto">
      <table class="w-full border-collapse text-left">
        <thead>
          <tr class="border-b border-line text-[11px] font-medium tracking-[0.08em] text-muted uppercase">
            <th scope="col" class="px-4 py-2 font-medium whitespace-nowrap">Дата</th>
            <th scope="col" class="px-4 py-2 font-medium whitespace-nowrap">Расписание</th>
            <th scope="col" class="px-4 py-2 font-medium whitespace-nowrap">Приезд</th>
            <th scope="col" class="px-4 py-2 font-medium whitespace-nowrap">Работа</th>
            <th scope="col" class="px-4 py-2 font-medium whitespace-nowrap">Завершено</th>
            <th scope="col" class="w-[1%] px-4 py-2"><span class="sr-only">Ссылка</span></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="v in visits" :key="v.scheduleId" class="border-b border-line last:border-b-0">
            <td class="mono px-4 py-2.5 whitespace-nowrap">{{ formatDate(v.targetDate) }}</td>
            <td class="px-4 py-2.5">
              <UiBadge :color="scheduleStatusColor(v.scheduleStatus.keyword)">
                {{ v.scheduleStatus.displayName }}
              </UiBadge>
            </td>
            <td class="mono px-4 py-2.5 whitespace-nowrap">{{ formatTime(v.arrivalTime) }}</td>
            <td class="mono px-4 py-2.5 whitespace-nowrap">{{ formatTime(v.startTime) }}–{{ formatTime(v.endTime) }}</td>
            <td class="mono px-4 py-2.5 whitespace-nowrap" :class="{ 'text-faint': !v.finishedAt }">
              {{ v.finishedAt ? formatDateTime(v.finishedAt) : "—" }}
            </td>
            <td class="px-4 py-2.5 whitespace-nowrap">
              <!-- the schedule page shows only today's schedule -->
              <RouterLink
                v-if="v.targetDate === today"
                :to="{ name: 'schedule', query: { region: regionId } }"
                class="text-accent no-underline hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
              >
                Открыть
              </RouterLink>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </UiPanel>
</template>

<script setup lang="ts">
import UiBadge from "@/components/ui/UiBadge.vue";
import UiEmptyState from "@/components/ui/UiEmptyState.vue";
import UiPanel from "@/components/ui/UiPanel.vue";
import { scheduleStatusColor } from "../model/colors";
import { formatDate, formatDateTime, formatTime } from "../model/format";
import type { ScheduledVisit } from "../types";

defineProps<{
  /** newest first */
  visits: ScheduledVisit[];
  regionId: number;
}>();

/** local "YYYY-MM-DD" — the server builds schedules for its local date */
const now = new Date();
const today = [now.getFullYear(), now.getMonth() + 1, now.getDate()].map((n) => String(n).padStart(2, "0")).join("-");
</script>
