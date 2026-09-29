<template>
  <RequestsBoard
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
import { useDictionariesStore } from "@/features/dictionaries/stores/dictionaries";
import RequestsBoard from "@/features/requests/components/RequestsBoard.vue";
import { useRequestsStore } from "@/features/requests/stores/requests";

const store = useRequestsStore();
const dictionariesStore = useDictionariesStore();

function changeLimit(limit: number | null): void {
  if (limit != null) store.setLimit(limit);
}

onMounted(() => {
  void store.init();
  // the status and job type filters, and the statuses' names in the table
  void dictionariesStore.load(["request-statuses", "job-types"]);
});
</script>
