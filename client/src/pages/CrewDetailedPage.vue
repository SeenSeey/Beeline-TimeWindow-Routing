<template>
  <CrewDetails
    v-if="state.kind === 'ready'"
    :crew="state.data"
    :off-shift-pending="store.offShiftPending"
    :off-shift-done="store.offShiftDone"
    :off-shift-error="store.offShiftError"
    @take-off-shift="store.takeOffShift(id)"
  />

  <CrewDetailsMessage v-else-if="state.kind === 'loading'" :id="id" kind="loading" />

  <CrewDetailsMessage
    v-else
    :id="id"
    :kind="state.kind"
    :message="state.message"
    :status="state.status"
    @retry="store.load(id)"
  />
</template>

<script setup lang="ts">
import { computed, watch } from "vue";
import CrewDetails from "@/features/crews/components/CrewDetails.vue";
import CrewDetailsMessage from "@/features/crews/components/CrewDetailsMessage.vue";
import { useCrewDetailsStore } from "@/features/crews/stores/crewDetails";

const props = defineProps<{
  /** from the route: /crews/:id */
  id: number;
}>();

const store = useCrewDetailsStore();

const state = computed(() => store.state);

// the page is reused when the route goes from one crew to another
watch(
  () => props.id,
  (id) => void store.load(id),
  { immediate: true },
);
</script>
