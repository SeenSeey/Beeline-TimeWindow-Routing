<template>
  <!-- the crew's requests, latest assignment first, as a compact list for the side column: number and state,
       the address, then the job and its dates -->
  <UiPanel title="Назначенные заявки">
    <template #aside>{{ requests.length }}</template>

    <UiEmptyState v-if="!requests.length">Бригаде ещё не назначали заявок.</UiEmptyState>

    <ul v-else class="m-0 grid max-h-[720px] list-none gap-2 overflow-y-auto p-3">
      <li v-for="request in requests" :key="request.id" class="grid gap-1.5 rounded-md bg-shift px-3 py-2.5">
        <div class="flex flex-wrap items-center gap-x-2 gap-y-1">
          <RouterLink
            :to="{ name: 'request', params: { id: request.id } }"
            class="mono rounded-sm text-sm font-semibold text-accent no-underline hover:underline focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
          >
            №{{ request.id }}
          </RouterLink>
          <UiBadge v-if="request.status" :color="statusColor(request.status.keyword)" dot>
            {{ request.status.displayName }}
          </UiBadge>
          <UiBadge v-if="request.urgent" color="var(--urgent)">Срочная</UiBadge>
        </div>

        <span class="text-sm text-ink">{{ request.fullAddress }}</span>

        <div class="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-muted">
          <UiBadge :color="jobTypeColor(request.jobType.keyword)">{{ request.jobType.displayName }}</UiBadge>
          <span>срок <span class="mono">{{ formatDateTime(request.deadline) }}</span></span>
          <span v-if="request.assignedAt">
            <span class="text-faint" aria-hidden="true">·</span> назначена
            <span class="mono">{{ formatDateTime(request.assignedAt) }}</span>
          </span>
        </div>
      </li>
    </ul>
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
</script>
