<template>
  <!-- keyed by region so scroll and tooltip start fresh for each schedule -->
  <TimelineBoard v-if="state.kind === 'ready'" :key="state.data.region.id" :schedule="state.data">
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
import { computed, onBeforeUnmount, onMounted } from "vue";
import BoardMessage from "@/features/timeline/components/BoardMessage.vue";
import RegionSelect from "@/features/timeline/components/RegionSelect.vue";
import TimelineBoard from "@/features/timeline/components/TimelineBoard.vue";
import { useRegionsStore } from "@/features/timeline/stores/regions";
import { useScheduleStore } from "@/features/timeline/stores/schedule";
import { useScheduleEditStore } from "@/features/timeline/stores/scheduleEdit";

const regionsStore = useRegionsStore();
const scheduleStore = useScheduleStore();
const scheduleEditStore = useScheduleEditStore();

const state = computed(() => scheduleStore.state);

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
});
</script>
