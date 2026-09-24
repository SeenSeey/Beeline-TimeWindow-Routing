<template>
  <!-- same page frame as the schedule board; the map takes the height left under the app header
       (h-14 + border) and the page padding (p-4 / sm:p-6) -->
  <div class="p-4 sm:p-6">
    <div
      class="grid h-[calc(100dvh-57px-2rem)] min-h-[480px] grid-rows-[auto_1fr] gap-4 sm:h-[calc(100dvh-57px-3rem)] sm:gap-6"
    >
      <header class="grid gap-4">
        <div class="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
          <div class="flex flex-wrap items-baseline gap-x-3 gap-y-1">
            <h1 class="m-0 text-2xl font-semibold text-ink">Карта заявок</h1>
            <span v-if="points" class="inline-flex items-center gap-2 text-sm text-muted">
              <UiDot v-if="switching" size="sm" color="var(--accent)" pulse />
              <span>
                <b class="mono font-medium text-ink">{{ visiblePoints.length }}</b>
                <template v-if="visiblePoints.length !== points.length"> из {{ points.length }}</template>
                заявок
              </span>
            </span>
          </div>
          <slot name="region" />
        </div>
        <!-- Preline toolbar card -->
        <div class="grid gap-3 rounded-lg border border-line bg-surface p-4 shadow-2xs">
          <RequestFiltersBar v-model:filters="filters" @reset="filters = { ...EMPTY_FILTERS }" />
          <MapLegend :job-types="jobTypes" />
        </div>
      </header>

      <div class="relative min-h-0 overflow-hidden rounded-lg border border-line bg-shift shadow-2xs">
        <RequestsMap
          :points="visiblePoints"
          :selected-id="selected?.id ?? null"
          :fit-key="fitKey"
          @select="selectedId = $event"
        />

        <RequestPointCard
          v-if="selected"
          :point="selected"
          class="absolute top-3 start-3 z-[1] w-[min(340px,calc(100%-24px))]"
          @close="selectedId = null"
        />

        <div
          v-if="state.kind === 'loading'"
          class="absolute inset-0 z-[2] grid place-items-center bg-[color-mix(in_srgb,var(--bg)_55%,transparent)]"
          role="status"
        >
          <span class="inline-flex items-center gap-2.5 rounded-lg border border-line bg-surface px-4 py-2 text-sm shadow-md">
            <UiDot color="var(--faint)" pulse />Загружаем заявки…
          </span>
        </div>

        <div
          v-else-if="state.kind !== 'ready'"
          class="absolute inset-0 z-[2] grid place-items-center bg-[color-mix(in_srgb,var(--bg)_70%,transparent)] p-5"
          role="alert"
        >
          <section
            class="grid max-w-[420px] justify-items-start gap-3 rounded-lg border border-line bg-surface p-5 shadow-md"
          >
            <div class="flex items-center gap-2.5">
              <UiDot color="var(--unassigned)" />
              <span class="text-sm font-medium text-ink">
                {{ state.kind === "not-found" ? "Регион не найден" : "Не удалось загрузить заявки" }}
              </span>
            </div>
            <p v-if="state.message" class="mono m-0 rounded-lg bg-shift px-3 py-2 text-xs text-ink">{{ state.message }}</p>
            <div class="flex items-center gap-4">
              <UiButton variant="solid" @click="emit('retry')">Обновить</UiButton>
              <span v-if="state.status" class="mono text-xs text-faint">HTTP {{ state.status }}</span>
            </div>
          </section>
        </div>

        <span
          v-else-if="!visiblePoints.length"
          class="absolute top-3 left-1/2 z-[1] -translate-x-1/2 rounded-lg border border-line bg-surface px-4 py-1.5 text-sm text-muted shadow-md"
        >
          {{ points?.length ? "Все заявки скрыты фильтром" : "В регионе нет заявок" }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import UiButton from "@/components/ui/UiButton.vue";
import UiDot from "@/components/ui/UiDot.vue";
import RequestFiltersBar from "@/features/requests/components/RequestFiltersBar.vue";
import { EMPTY_FILTERS } from "@/features/requests/model/query";
import type { RequestFilters } from "@/features/requests/types";
import { collectJobTypes } from "@/features/timeline/model/colors";
import { matchesFilters } from "../model/points";
import type { RequestPointsLoadState } from "../stores/requestPoints";
import MapLegend from "./MapLegend.vue";
import RequestPointCard from "./RequestPointCard.vue";
import RequestsMap from "./RequestsMap.vue";

const props = defineProps<{
  state: RequestPointsLoadState;
  /** a new region's points are loading; the current ones stay on the map */
  switching: boolean;
  /** the region the shown points belong to; the map re-fits when it changes */
  regionId: number | null;
}>();

const emit = defineEmits<{
  retry: [];
}>();

/** kept across region switches; the region itself is chosen by the page's region select */
const filters = ref<RequestFilters>({ ...EMPTY_FILTERS });
const selectedId = ref<number | null>(null);

const points = computed(() => (props.state.kind === "ready" ? props.state.points : null));
const visiblePoints = computed(() => (points.value ?? []).filter((p) => matchesFilters(p, filters.value)));
const selected = computed(() => visiblePoints.value.find((p) => p.id === selectedId.value) ?? null);
const jobTypes = computed(() => collectJobTypes((points.value ?? []).map((p) => p.jobType)));

/** re-fit once the new region's points are in, not when the region is merely picked */
const fitKey = computed(() => (points.value && !props.switching ? props.regionId : null));

watch(points, () => (selectedId.value = null));
</script>
