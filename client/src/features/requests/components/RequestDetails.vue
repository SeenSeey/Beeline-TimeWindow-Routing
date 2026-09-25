<template>
  <!-- same page frame as the schedule board (timeline BoardLayout) -->
  <div class="grid grid-cols-[minmax(0,1fr)] content-start gap-4 p-4 sm:gap-6 sm:p-6">
    <!-- 1. who and where: number, state, address -->
    <header class="grid gap-2">
      <RouterLink
        :to="{ name: 'requests' }"
        class="inline-flex w-fit items-center gap-x-1.5 rounded-sm text-sm text-muted no-underline hover:text-accent focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
      >
        <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <path d="m15 18-6-6 6-6" />
        </svg>
        Все заявки
      </RouterLink>

      <div class="flex flex-wrap items-start justify-between gap-x-6 gap-y-3">
        <div class="grid gap-2">
          <div class="flex flex-wrap items-center gap-x-3 gap-y-2">
            <h1 class="m-0 text-2xl font-semibold text-balance text-ink">
              Заявка <span class="mono">№{{ request.id }}</span>
            </h1>
            <UiBadge v-if="request.status" size="md" :color="statusColor(request.status.keyword)" dot>
              {{ request.status.displayName }}
            </UiBadge>
            <UiBadge v-if="request.urgent" size="md" color="var(--urgent)">Срочная</UiBadge>
          </div>

          <p class="m-0 text-sm text-muted">
            {{ request.fullAddress }}
            <span class="text-faint"> · </span>
            {{ request.region.displayName }}
          </p>
        </div>

        <!-- start work / complete / cancel, as the request's status allows -->
        <RequestStatusActions
          :request="request"
          :pending-action="pendingAction"
          :error="actionError"
          @change="emit('changeStatus', $event)"
        />
      </div>
    </header>

    <!-- 2. at a glance -->
    <RequestKeyFacts :request="request" />

    <!-- 3. details: the request's story and work on the left, place and crew on the side -->
    <div class="grid grid-cols-1 items-start gap-4 sm:gap-6 xl:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
      <div class="grid min-w-0 gap-4 sm:gap-6">
        <RequestLifecyclePanel :request="request" />
        <RequestRequirementsPanel :request="request" />
        <RequestVisitsPanel :visits="request.scheduledVisits" :region-id="request.region.id" />
      </div>

      <div class="grid min-w-0 gap-4 sm:gap-6">
        <UiPanel title="Место">
          <UiDetailList :rows="placeRows" />
        </UiPanel>
        <RequestCrewPanel :crew="request.assignedCrew" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiBadge from "@/components/ui/UiBadge.vue";
import UiDetailList, { type UiDetailRow } from "@/components/ui/UiDetailList.vue";
import UiPanel from "@/components/ui/UiPanel.vue";
import { statusColor } from "../model/colors";
import type { RequestStatusAction } from "../api/requestsApi";
import type { RequestDetailed } from "../types";
import RequestCrewPanel from "./RequestCrewPanel.vue";
import RequestKeyFacts from "./RequestKeyFacts.vue";
import RequestLifecyclePanel from "./RequestLifecyclePanel.vue";
import RequestRequirementsPanel from "./RequestRequirementsPanel.vue";
import RequestStatusActions from "./RequestStatusActions.vue";
import RequestVisitsPanel from "./RequestVisitsPanel.vue";

const props = withDefaults(
  defineProps<{
    request: RequestDetailed;
    /** the status change in flight */
    pendingAction?: RequestStatusAction | null;
    /** why the last status change failed */
    actionError?: string | null;
  }>(),
  { pendingAction: null, actionError: null },
);

const emit = defineEmits<{
  changeStatus: [action: RequestStatusAction];
}>();

const placeRows = computed<UiDetailRow[]>(() => {
  const request = props.request;
  return [
    { label: "Адрес", value: request.fullAddress },
    { label: "Регион", value: request.region.displayName },
    { label: "Офис региона", value: request.region.officeAddress },
    { label: "Координаты", value: `${request.latitude.toFixed(5)}, ${request.longitude.toFixed(5)}`, mono: true },
  ];
});
</script>
