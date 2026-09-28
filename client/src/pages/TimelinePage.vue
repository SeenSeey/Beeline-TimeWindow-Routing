<template>
  <!-- keyed by region so scroll and tooltip start fresh for each schedule -->
  <!-- the board shows the current version or, read-only, an earlier one (scheduleStore.schedule) -->
  <TimelineBoard
    v-if="state.kind === 'ready'"
    :key="state.data.region.id"
    :schedule="scheduleStore.schedule ?? state.data"
    :historical="scheduleStore.viewingHistory"
    :crew-route="crewRouteStore.route"
  >
    <template #versions>
      <ScheduleVersionBar
        :schedule="scheduleStore.schedule ?? state.data"
        :historical="scheduleStore.viewingHistory"
        :steps-back="scheduleStore.versionTrail.length"
        :loading="scheduleStore.versionLoading"
        :error="scheduleStore.versionError"
        :disabled="scheduleEditStore.active"
        @older="scheduleStore.showOlderVersion()"
        @newer="scheduleStore.showNewerVersion()"
        @current="scheduleStore.showCurrentVersion()"
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
import { useRequestCreateStore } from "@/features/requests/stores/requestCreate";
import BoardMessage from "@/features/timeline/components/BoardMessage.vue";
import RegionSelect from "@/features/timeline/components/RegionSelect.vue";
import ScheduleVersionBar from "@/features/timeline/components/ScheduleVersionBar.vue";
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
  // a string, so a reload of the same schedule with the same crew selected doesn't refetch; a saved edit
  // (the store's revision) does: it rebuilt the edited crews' routes under the same schedule id
  () => {
    const schedule = scheduleStore.schedule;
    const crew = boardStore.selectedCrew;
    return schedule && crew && crew.routeDistanceKilometers != null
      ? `${schedule.id}:${scheduleStore.revision}:${crew.id}`
      : null;
  },
  (target) => {
    if (!target) {
      crewRouteStore.clear();
      return;
    }
    const [scheduleId, revision, crewId] = target.split(":").map(Number);
    void crewRouteStore.load(scheduleId!, crewId!, revision);
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

// A request created from the header: one of the shown region lands in its unassigned list (an urgent one
// makes the server recalculate the schedule)
watch(
  () => requestCreateStore.lastCreated,
  (created) => {
    if (created && created.region.id === regionsStore.regionId) void scheduleStore.load(created.region.id);
  },
);

function retry(): void {
  if (regionsStore.regions.length && regionsStore.regionId != null) void scheduleStore.load(regionsStore.regionId);
  else void init();
}

onMounted(init);
// the stores outlive the page: don't keep refetching a calculating schedule nobody looks at, and don't
// carry an edit draft over to a schedule that will be loaded afresh next time
onBeforeUnmount(() => {
  scheduleStore.stopPolling();
  // the next visit starts from the current version
  scheduleStore.showCurrentVersion();
  scheduleEditStore.stop();
  scheduleEditStore.dismissNotice();
});
</script>
