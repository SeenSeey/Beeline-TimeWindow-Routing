<template>
  <UiPanel title="Назначенные заявки">
    <template #aside>{{ requests.length }}</template>

    <UiEmptyState v-if="!requests.length">Бригаде ещё не назначали заявок.</UiEmptyState>

    <!-- Preline table: gray head with small uppercase labels, divided rows -->
    <div v-else class="max-h-[480px] overflow-auto">
      <table class="min-w-full text-start">
        <thead>
          <tr>
            <th
              v-for="column in COLUMNS"
              :key="column"
              scope="col"
              class="sticky top-0 z-[1] bg-canvas px-5 py-3 text-start text-xs font-semibold tracking-wide whitespace-nowrap text-ink uppercase shadow-[inset_0_-1px_0_var(--line)]"
            >
              {{ column }}
            </th>
          </tr>
        </thead>
        <tbody class="divide-y divide-line text-sm">
          <tr v-for="request in requests" :key="request.id" class="hover:bg-canvas">
            <td class="px-5 py-3">
              <RouterLink
                :to="{ name: 'request', params: { id: request.id } }"
                class="mono rounded-sm font-medium text-accent no-underline hover:underline focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
              >
                {{ request.id }}
              </RouterLink>
            </td>
            <td class="px-5 py-3 text-ink">{{ request.fullAddress }}</td>
            <td class="px-5 py-3">
              <div class="flex flex-wrap gap-1.5">
                <UiBadge v-if="request.status" :color="statusColor(request.status.keyword)" dot>
                  {{ request.status.displayName }}
                </UiBadge>
                <UiBadge v-if="request.urgent" color="var(--urgent)">Срочная</UiBadge>
              </div>
            </td>
            <td class="px-5 py-3">
              <UiBadge :color="jobTypeColor(request.jobType.keyword)">{{ request.jobType.displayName }}</UiBadge>
            </td>
            <td class="mono px-5 py-3 whitespace-nowrap text-muted">{{ formatDateTime(request.deadline) }}</td>
            <td class="mono px-5 py-3 whitespace-nowrap" :class="request.assignedAt ? 'text-muted' : 'text-faint'">
              {{ request.assignedAt ? formatDateTime(request.assignedAt) : "—" }}
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
import { jobTypeColor, statusColor } from "@/features/requests/model/colors";
import { formatDateTime } from "@/features/timeline/model/format";
import type { CrewAssignedRequest } from "../types";

defineProps<{
  /** latest assignment first */
  requests: CrewAssignedRequest[];
}>();

const COLUMNS = ["№", "Адрес", "Статус", "Тип работ", "Крайний срок", "Назначена"];
</script>
