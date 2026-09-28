<template>
  <MapBoard
    :state="pointsStore.state"
    :switching="pointsStore.switching"
    :region-id="regionsStore.regionId"
    @retry="retry"
  >
    <template v-if="regionsStore.regions.length" #region>
      <RegionSelect
        :model-value="regionsStore.regionId"
        :regions="regionsStore.regions"
        :busy="pointsStore.switching"
        @update:model-value="changeRegion"
      />
    </template>
  </MapBoard>
</template>

<script setup lang="ts">
import { onMounted, watch } from "vue";
import MapBoard from "@/features/map/components/MapBoard.vue";
import { useRequestPointsStore } from "@/features/map/stores/requestPoints";
import { useRequestCreateStore } from "@/features/requests/stores/requestCreate";
import RegionSelect from "@/features/timeline/components/RegionSelect.vue";
import { useRegionsStore } from "@/features/timeline/stores/regions";

const regionsStore = useRegionsStore();
const pointsStore = useRequestPointsStore();
const requestCreateStore = useRequestCreateStore();

/** Regions first (they decide which requests to show), then that region's requests */
async function init(): Promise<void> {
  pointsStore.reset();
  try {
    await regionsStore.load();
  } catch (e) {
    pointsStore.fail(e);
    return;
  }
  if (regionsStore.regionId == null) {
    pointsStore.failWith({ kind: "not-found", message: "Нет ни одного региона", status: null });
    return;
  }
  await pointsStore.load(regionsStore.regionId);
}

function changeRegion(id: number | null): void {
  if (id == null || id === regionsStore.regionId) return;
  regionsStore.select(id);
  void pointsStore.load(id);
}

// A request created from the header: one of the shown region gets its point on the map
watch(
  () => requestCreateStore.lastCreated,
  (created) => {
    if (created && created.region.id === regionsStore.regionId) void pointsStore.load(created.region.id);
  },
);

function retry(): void {
  if (regionsStore.regions.length && regionsStore.regionId != null) void pointsStore.load(regionsStore.regionId);
  else void init();
}

onMounted(init);
</script>
