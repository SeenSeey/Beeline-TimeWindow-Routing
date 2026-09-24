<template>
  <UiPanel title="В расписаниях">
    <template #aside>{{ visits.length }}</template>

    <UiEmptyState v-if="!visits.length">Заявка пока не попадала ни в одно расписание.</UiEmptyState>

    <div v-else class="overflow-x-auto">
      <!-- Preline table: gray head with small uppercase labels, divided rows -->
      <table class="min-w-full divide-y divide-line text-start">
        <thead class="bg-canvas">
          <tr class="text-xs font-semibold tracking-wide text-ink uppercase">
            <th scope="col" class="px-5 py-3 text-start whitespace-nowrap">Дата</th>
            <th scope="col" class="px-5 py-3 text-start whitespace-nowrap">Расписание</th>
            <th scope="col" class="px-5 py-3 text-start whitespace-nowrap">Приезд</th>
            <th scope="col" class="px-5 py-3 text-start whitespace-nowrap">Работа</th>
            <th scope="col" class="px-5 py-3 text-start whitespace-nowrap">Завершено</th>
            <th scope="col" class="w-[1%] px-5 py-3"><span class="sr-only">Ссылка</span></th>
          </tr>
        </thead>
        <tbody class="divide-y divide-line text-sm">
          <tr v-for="v in visits" :key="v.scheduleId" class="hover:bg-canvas">
            <td class="mono px-5 py-3 whitespace-nowrap text-ink">{{ formatDate(v.targetDate) }}</td>
            <td class="px-5 py-3">
              <UiBadge :color="scheduleStatusColor(v.scheduleStatus.keyword)">
                {{ v.scheduleStatus.displayName }}
              </UiBadge>
            </td>
            <td class="mono px-5 py-3 whitespace-nowrap text-muted">{{ formatTime(v.arrivalTime) }}</td>
            <td class="mono px-5 py-3 whitespace-nowrap text-muted">
              {{ formatTime(v.startTime) }}–{{ formatTime(v.endTime) }}
            </td>
            <td class="mono px-5 py-3 whitespace-nowrap" :class="v.finishedAt ? 'text-muted' : 'text-faint'">
              {{ v.finishedAt ? formatDateTime(v.finishedAt) : "—" }}
            </td>
            <td class="px-5 py-3 whitespace-nowrap">
              <!-- the schedule page shows only today's schedule -->
              <RouterLink
                v-if="v.targetDate === today"
                :to="{ name: 'schedule', query: { region: regionId } }"
                class="rounded-sm font-medium text-accent no-underline hover:underline focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
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
