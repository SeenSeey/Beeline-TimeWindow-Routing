<template>
  <RequestDetails
    v-if="state.kind === 'ready'"
    :request="state.data"
    :pending-action="store.pendingAction"
    :action-error="store.actionError"
    @change-status="store.changeStatus(id, $event)"
  />

  <RequestDetailsMessage v-else-if="state.kind === 'loading'" :id="id" kind="loading" />

  <RequestDetailsMessage
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
import RequestDetails from "@/features/requests/components/RequestDetails.vue";
import RequestDetailsMessage from "@/features/requests/components/RequestDetailsMessage.vue";
import { useRequestDetailsStore } from "@/features/requests/stores/requestDetails";

const props = defineProps<{
  /** from the route: /requests/:id */
  id: number;
}>();

const store = useRequestDetailsStore();

const state = computed(() => store.state);

// the page is reused when the route goes from one request to another
watch(
  () => props.id,
  (id) => void store.load(id),
  { immediate: true },
);
</script>
