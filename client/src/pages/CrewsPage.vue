<template>
  <CrewsBoard
    :state="store.state"
    :switching="store.switching"
    :regions="store.regions"
    :filters="store.query.filters"
    :limit="store.query.limit"
    :sort="store.query.sort"
    @update:filters="store.setFilters"
    @sort="store.toggleSort"
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
import { useDictionariesStore } from "@/features/dictionaries/stores/dictionaries";

const store = useCrewsStore();
const dictionariesStore = useDictionariesStore();

function changeLimit(limit: number | null): void {
  if (limit != null) store.setLimit(limit);
}

onMounted(() => {
  void store.init();
  // the vehicle and skill filters, and their names in the table
  void dictionariesStore.load(["vehicles", "skills"]);
});
</script>
