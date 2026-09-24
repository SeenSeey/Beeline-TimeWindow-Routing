<template>
  <CrewsBoard
    :state="store.state"
    :switching="store.switching"
    :regions="store.regions"
    :filters="store.query.filters"
    :limit="store.query.limit"
    @update:filters="store.setFilters"
    @update:limit="changeLimit"
    @update:page="store.setPage"
    @reset="store.resetFilters"
    @retry="store.load"
  />
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import CrewsBoard from "@/features/crews/components/CrewsBoard.vue";
import { useCrewsStore } from "@/features/crews/stores/crews";

const store = useCrewsStore();

function changeLimit(limit: number | null): void {
  if (limit != null) store.setLimit(limit);
}

onMounted(() => void store.init());
</script>
