<template>
  <SystemBoard
    :results="store.results"
    :busy="store.busy"
    @initialize="store.initialize"
    @reset="store.reset"
  />
</template>

<script setup lang="ts">
import SystemBoard from "@/features/system/components/SystemBoard.vue";
import { useSystemActionsStore } from "@/features/system/stores/systemActions";

const store = useSystemActionsStore();
</script>
