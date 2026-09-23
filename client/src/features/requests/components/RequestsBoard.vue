<template>
  <!-- same page frame as the schedule board (timeline BoardLayout) -->
  <div class="px-5 pt-5 pb-10">
    <div class="mx-auto grid max-w-[1440px] grid-cols-[minmax(0,1fr)] gap-5">
      <UiPanel title="Заявки">
        <template #aside>
          <span v-if="data" class="inline-flex items-center gap-2">
            <UiDot v-if="switching" size="sm" color="var(--accent)" pulse />
            Всего: <b class="mono font-medium text-ink">{{ data.totalElements }}</b>
          </span>
        </template>

        <div class="border-b border-line px-4 py-3">
          <RequestFiltersBar v-model:filters="filters" :regions="regions" @reset="emit('reset')" />
        </div>

        <div v-if="state.kind === 'loading'" class="flex items-center gap-2.5 p-4 text-muted" role="status">
          <UiDot color="var(--faint)" pulse />
          Загружаем заявки…
        </div>

        <div v-else-if="state.kind !== 'ready'" class="grid justify-items-start gap-3 p-4" role="alert">
          <div class="flex items-center gap-2.5">
            <UiDot color="var(--unassigned)" />
            <span class="font-medium">Не удалось загрузить заявки</span>
          </div>
          <p v-if="state.message" class="mono m-0 rounded-md bg-shift px-3 py-2 text-[12.5px]">{{ state.message }}</p>
          <div class="flex items-center gap-4">
            <UiButton variant="solid" @click="emit('retry')">Обновить</UiButton>
            <span v-if="state.status" class="mono text-[12px] text-faint">HTTP {{ state.status }}</span>
          </div>
        </div>

        <UiEmptyState v-else-if="!state.data.payload.length">
          {{ hasActiveFilters(filters) ? "Нет заявок, подходящих под фильтры." : "Заявок пока нет." }}
        </UiEmptyState>

        <RequestsTable
          v-else
          :requests="state.data.payload"
          class="motion-safe:transition-opacity"
          :class="{ 'opacity-60': switching }"
          :aria-busy="switching"
        />

        <div
          v-if="data && data.totalElements > 0"
          class="flex flex-wrap items-center justify-between gap-x-4 gap-y-2.5 border-t border-line px-4 py-3"
        >
          <UiPagination
            :page="data.page"
            :total-pages="data.totalPages"
            :disabled="switching"
            @update:page="emit('update:page', $event)"
          />
          <div class="flex items-center gap-4 text-[12px] text-muted">
            <span class="mono">{{ rangeLabel }}</span>
            <UiSelect v-model="limit" label="На странице" :options="limitOptions" />
          </div>
        </div>
      </UiPanel>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiButton from "@/components/ui/UiButton.vue";
import UiDot from "@/components/ui/UiDot.vue";
import UiEmptyState from "@/components/ui/UiEmptyState.vue";
import UiPagination from "@/components/ui/UiPagination.vue";
import UiPanel from "@/components/ui/UiPanel.vue";
import UiSelect from "@/components/ui/UiSelect.vue";
import { hasActiveFilters, PAGE_SIZES } from "../model/query";
import type { RequestsLoadState } from "../stores/requests";
import type { RegionInfo, RequestFilters } from "../types";
import RequestFiltersBar from "./RequestFiltersBar.vue";
import RequestsTable from "./RequestsTable.vue";

const props = defineProps<{
  state: RequestsLoadState;
  /** a new page or filter set is loading over the current rows */
  switching: boolean;
  regions: RegionInfo[];
}>();

const filters = defineModel<RequestFilters>("filters", { required: true });
const limit = defineModel<number | null>("limit", { required: true });

const emit = defineEmits<{
  "update:page": [page: number];
  reset: [];
  retry: [];
}>();

const limitOptions = PAGE_SIZES.map((n) => ({ value: n as number, label: String(n) }));

const data = computed(() => (props.state.kind === "ready" ? props.state.data : null));

/** "21–40 из 134" */
const rangeLabel = computed(() => {
  if (!data.value) return "";
  const { page, limit: size, totalElements, payload } = data.value;
  const from = (page - 1) * size + 1;
  return `${from}–${from + payload.length - 1} из ${totalElements}`;
});
</script>
