<template>
  <UiPanel title="Не вошли в расписание">
    <template #aside>
      <span class="inline-flex flex-wrap items-center gap-2">
        <span><span class="mono">{{ requests.length }}</span> заявок</span>
        <!-- emergencies left out are the first thing a dispatcher needs to see -->
        <UiBadge
          v-if="emergencyCount"
          :color="jobTypeColor('EMERGENCY')"
          dot
          :title="`Не вошли в расписание: заявок типа «${emergencyLabel}» — ${emergencyCount}`"
        >
          аварийных: <span class="mono">{{ emergencyCount }}</span>
        </UiBadge>
      </span>
    </template>

    <UiEmptyState v-if="!requests.length">Все активные заявки региона вошли в расписание.</UiEmptyState>

    <!-- scrolls past about the height of the crew's route panel beside it (its 840px map, the distance
         line), so the two panels end together -->
    <ul v-else class="m-0 grid max-h-[880px] list-none gap-2 overflow-y-auto p-3">
      <!-- the server orders them urgent first, then by deadline -->
      <UnassignedRequestItem v-for="r in requests" :key="r.id" :request="r" :target-date="targetDate" />
    </ul>
  </UiPanel>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiBadge from "@/components/ui/UiBadge.vue";
import UiEmptyState from "@/components/ui/UiEmptyState.vue";
import UiPanel from "@/components/ui/UiPanel.vue";
import { jobTypeColor } from "../model/colors";
import type { UnassignedRequest } from "../types";
import UnassignedRequestItem from "./UnassignedRequestItem.vue";

const props = defineProps<{
  /** shown only for a calculated schedule: while calculating the server sends none */
  requests: UnassignedRequest[];
  targetDate: string;
}>();

/** requests of the EMERGENCY job type among those left out */
const emergencies = computed(() => props.requests.filter((request) => request.jobType.keyword === "EMERGENCY"));
const emergencyCount = computed(() => emergencies.value.length);
/** the job type's display name, as the server sends it */
const emergencyLabel = computed(() => emergencies.value[0]?.jobType.displayName ?? "аварийные");
</script>
