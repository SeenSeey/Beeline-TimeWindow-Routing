<template>
  <UiPanel title="Не вошли в расписание">
    <template #aside>{{ requests.length }} заявок</template>

    <UiEmptyState v-if="!ready">Список появится, когда расписание будет сформировано.</UiEmptyState>
    <UiEmptyState v-else-if="!requests.length">Все активные заявки региона вошли в расписание.</UiEmptyState>

    <ul v-else class="m-0 max-h-[640px] list-none overflow-y-auto py-1.5">
      <!-- the server orders them urgent first, then by deadline -->
      <UnassignedRequestItem v-for="r in requests" :key="r.id" :request="r" :target-date="targetDate" />
    </ul>
  </UiPanel>
</template>

<script setup lang="ts">
import UiEmptyState from "@/components/ui/UiEmptyState.vue";
import UiPanel from "@/components/ui/UiPanel.vue";
import type { UnassignedRequest } from "../types";
import UnassignedRequestItem from "./UnassignedRequestItem.vue";

defineProps<{
  requests: UnassignedRequest[];
  /** the schedule has been calculated; before that the list is always empty */
  ready: boolean;
  targetDate: string;
}>();
</script>
