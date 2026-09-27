<template>
  <MapBoard
    :state="pointsStore.state"
    :switching="pointsStore.switching"
    :region-id="regionsStore.regionId"
    @retry="retry"
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
import { onBeforeUnmount, onMounted } from "vue";
import MapBoard from "@/features/map/components/MapBoard.vue";
import { useRequestPointsStore } from "@/features/map/stores/requestPoints";
import RequestCreateAction from "@/features/requests/components/RequestCreateAction.vue";
import { useRequestCreateStore } from "@/features/requests/stores/requestCreate";
import type { RequestCreateBody } from "@/features/requests/types";
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

/** A new request of the shown region gets its point on the map */
async function createRequest(body: RequestCreateBody): Promise<void> {
  const created = await requestCreateStore.create(body);
  if (created && body.regionId === regionsStore.regionId) void pointsStore.load(body.regionId);
}

function retry(): void {
  if (regionsStore.regions.length && regionsStore.regionId != null) void pointsStore.load(regionsStore.regionId);
  else void init();
}

onMounted(init);
// the store outlives the page: don't reopen the dialog on the next visit
onBeforeUnmount(() => requestCreateStore.closeDialog());
</script>
