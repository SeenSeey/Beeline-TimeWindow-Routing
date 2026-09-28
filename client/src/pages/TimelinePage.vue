<template>
  <!-- keyed by region so scroll and tooltip start fresh for each schedule -->
  <TimelineBoard
    v-if="state.kind === 'ready'"
    :key="state.data.region.id"
    :schedule="state.data"
    :crew-route="crewRouteStore.route"
  >
    <template #actions>
      <RequestCreateAction
        :regions="regionsStore.regions"
        :region-id="regionsStore.regionId"
        :open="requestCreateStore.open"
        :pending="requestCreateStore.pending"
        :error="requestCreateStore.error"
        @open="requestCreateStore.openDialog()"
        @close="requestCreateStore.closeDialog()"
        @submit="createRequest"
      />
    </template>
    <template #region>
      <RegionSelect
        :model-value="regionsStore.regionId"
        :regions="regionsStore.regions"
        :busy="scheduleStore.switching"
        @update:model-value="changeRegion"
      />
    </template>
  </TimelineBoard>

  <BoardMessage v-else-if="state.kind === 'loading'" kind="loading" />

  <BoardMessage v-else :kind="state.kind" :message="state.message" :status="state.status" @retry="retry">
    <template v-if="regionsStore.regions.length" #region>
      <RegionSelect
        :model-value="regionsStore.regionId"
        :regions="regionsStore.regions"
        :busy="scheduleStore.switching"
        @update:model-value="changeRegion"
      />
    </template>
  </BoardMessage>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, watch } from "vue";
import RequestCreateAction from "@/features/requests/components/RequestCreateAction.vue";
import { useRequestCreateStore } from "@/features/requests/stores/requestCreate";
import type { RequestCreateBody } from "@/features/requests/types";
import BoardMessage from "@/features/timeline/components/BoardMessage.vue";
import RegionSelect from "@/features/timeline/components/RegionSelect.vue";
import TimelineBoard from "@/features/timeline/components/TimelineBoard.vue";
import { useBoardStore } from "@/features/timeline/stores/board";
import { useCrewRouteStore } from "@/features/timeline/stores/crewRoute";
import { useRegionsStore } from "@/features/timeline/stores/regions";
import { useScheduleStore } from "@/features/timeline/stores/schedule";
import { useScheduleEditStore } from "@/features/timeline/stores/scheduleEdit";

const regionsStore = useRegionsStore();
const scheduleStore = useScheduleStore();
const scheduleEditStore = useScheduleEditStore();
const requestCreateStore = useRequestCreateStore();
const boardStore = useBoardStore();
const crewRouteStore = useCrewRouteStore();

const state = computed(() => scheduleStore.state);

// The selected crew's road route, for its map. Only a crew with a stored route (its row has a length) is
// asked for — that also leaves out a calculating schedule and the crews changed in edit mode
watch(
  // a string, so a reload of the same schedule with the same crew selected doesn't refetch
  () => {
    const schedule = scheduleStore.schedule;
    const crew = boardStore.selectedCrew;
    return schedule && crew && crew.routeDistanceKilometers != null ? `${schedule.id}:${crew.id}` : null;
  },
  (target) => {
    if (!target) {
      crewRouteStore.clear();
      return;
    }
    const [scheduleId, crewId] = target.split(":").map(Number);
    void crewRouteStore.load(scheduleId!, crewId!);
  },
  { immediate: true },
);

/** Regions first (they decide which schedule to show), then that region's schedule */
async function init(): Promise<void> {
  scheduleStore.reset();
  try {
    await regionsStore.load();
  } catch (e) {
    scheduleStore.fail(e);
    return;
  }
  if (regionsStore.regionId == null) {
    scheduleStore.failWith({ kind: "not-found", message: "Нет ни одного региона", status: null });
    return;
  }
  await scheduleStore.load(regionsStore.regionId);
}

function changeRegion(id: number | null): void {
  if (id == null || id === regionsStore.regionId) return;
  regionsStore.select(id);
  void scheduleStore.load(id);
}

/** A new request of the shown region lands in its unassigned list; an urgent one makes the server recalculate */
async function createRequest(body: RequestCreateBody): Promise<void> {
  const created = await requestCreateStore.create(body);
  if (created && body.regionId === regionsStore.regionId) void scheduleStore.load(body.regionId);
}

function retry(): void {
  if (regionsStore.regions.length && regionsStore.regionId != null) void scheduleStore.load(regionsStore.regionId);
  else void init();
}

onMounted(init);
// the stores outlive the page: don't keep refetching a calculating schedule nobody looks at, and don't
// carry an edit draft over to a schedule that will be loaded afresh next time
onBeforeUnmount(() => {
  scheduleStore.stopPolling();
  scheduleEditStore.stop();
  scheduleEditStore.dismissNotice();
  requestCreateStore.closeDialog();
});
</script>
