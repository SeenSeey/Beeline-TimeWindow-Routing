<template>
  <!-- Fills the viewport under the app header (h-14 + border): the card takes the remaining height,
       only the table scrolls inside it, so the pagination bar is always on screen -->
  <div
    class="grid h-[calc(100dvh-57px)] min-h-80 grid-cols-[minmax(0,1fr)] grid-rows-[auto_minmax(0,1fr)] gap-4 p-4 sm:gap-6 sm:p-6"
  >
    <h1 class="m-0 text-2xl font-semibold text-ink">Заявки</h1>

    <UiPanel title="Список заявок" class="min-h-0">
      <template #aside>
        <span v-if="data" class="inline-flex items-center gap-2">
          <UiDot v-if="switching" size="sm" color="var(--accent)" pulse />
          Всего: <b class="mono font-medium text-ink">{{ data.totalElements }}</b>
        </span>
      </template>

      <div class="shrink-0 border-b border-line px-5 py-4">
        <RequestFiltersBar v-model:filters="filters" :regions="regions" @reset="emit('reset')" />
      </div>

      <div
        v-if="state.kind === 'loading'"
        class="flex flex-1 items-center gap-2.5 px-5 py-8 text-sm text-muted"
        role="status"
      >
        <UiDot color="var(--faint)" pulse />
        Загружаем заявки…
      </div>

      <div v-else-if="state.kind !== 'ready'" class="grid flex-1 content-start justify-items-start gap-3 overflow-auto px-5 py-6" role="alert">
          <div class="flex items-center gap-2.5">
            <UiDot color="var(--unassigned)" />
            <span class="text-sm font-medium text-ink">Не удалось загрузить заявки</span>
          </div>
          <p v-if="state.message" class="mono m-0 rounded-lg bg-shift px-3 py-2 text-xs text-ink">{{ state.message }}</p>
          <div class="flex items-center gap-4">
            <UiButton variant="solid" @click="emit('retry')">Обновить</UiButton>
            <span v-if="state.status" class="mono text-xs text-faint">HTTP {{ state.status }}</span>
          </div>
        </div>

        <UiEmptyState v-else-if="!state.data.payload.length" class="flex-1">
          {{ hasActiveFilters(filters) ? "Нет заявок, подходящих под фильтры." : "Заявок пока нет." }}
        </UiEmptyState>

        <!-- the only scrolling part of the card -->
        <RequestsTable
          v-else
          :requests="state.data.payload"
          :sort="sort"
          class="min-h-0 flex-1 motion-safe:transition-opacity"
          :class="{ 'opacity-60': switching }"
          :aria-busy="switching"
          @sort="emit('sort', $event)"
        />

        <div
          v-if="data && data.totalElements > 0"
          class="flex shrink-0 flex-wrap items-center justify-between gap-x-4 gap-y-2 border-t border-line px-5 py-3"
        >
          <p class="mono m-0 text-sm text-muted">{{ rangeLabel }}</p>
          <div class="flex flex-wrap items-center gap-x-4 gap-y-2">
            <UiSelect v-model="limit" label="На странице" :options="limitOptions" />
            <UiPagination
              :page="data.page"
              :total-pages="data.totalPages"
              :disabled="switching"
              @update:page="emit('update:page', $event)"
            />
          </div>
        </div>
    </UiPanel>
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
import type { SortOrder } from "@/utils/sort";
import { hasActiveFilters, PAGE_SIZES, type RequestSortField } from "../model/query";
import type { RequestsLoadState } from "../stores/requests";
import type { RegionInfo, RequestFilters } from "../types";
import RequestFiltersBar from "./RequestFiltersBar.vue";
import RequestsTable from "./RequestsTable.vue";

const props = defineProps<{
  state: RequestsLoadState;
  /** a new page or filter set is loading over the current rows */
  switching: boolean;
  regions: RegionInfo[];
  /** the list's order; null: by number */
  sort: SortOrder<RequestSortField> | null;
}>();

const filters = defineModel<RequestFilters>("filters", { required: true });
const limit = defineModel<number | null>("limit", { required: true });

const emit = defineEmits<{
  "update:page": [page: number];
  reset: [];
  retry: [];
  /** a column head clicked */
  sort: [field: RequestSortField];
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
