<template>
  <div class="flex flex-wrap items-center gap-x-4 gap-y-2.5">
    <UiSelect v-model="status" label="Статус" :options="statusOptions" />
    <UiSelect v-model="jobType" label="Тип работ" :options="jobTypeOptions" />
    <UiSelect v-if="regions" v-model="region" label="Регион" :options="regionOptions" />
    <UiSelect v-model="urgent" label="Срочность" :options="URGENT_OPTIONS" />
    <UiSelect v-model="crew" label="Бригада" :options="CREW_OPTIONS" />
    <UiButton v-if="hasActiveFilters(filters)" @click="emit('reset')">Сбросить</UiButton>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiButton from "@/components/ui/UiButton.vue";
import UiSelect from "@/components/ui/UiSelect.vue";
import { JOB_TYPES, REQUEST_STATUSES } from "../model/dictionaries";
import { hasActiveFilters } from "../model/query";
import type { RegionInfo, RequestFilters } from "../types";

const props = defineProps<{
  /** options of the region filter; without them the filter is hidden (the map has its own region select) */
  regions?: RegionInfo[];
}>();

const filters = defineModel<RequestFilters>("filters", { required: true });

const emit = defineEmits<{
  reset: [];
}>();

// UiSelect needs a real option for "no filter"; `ANY` stands for null
const ANY = "any" as const;
type Choice<T> = T | typeof ANY;

const statusOptions = [{ value: ANY, label: "Все" }, ...REQUEST_STATUSES.map((s) => ({ value: s.id, label: s.label }))];
const jobTypeOptions = [{ value: ANY, label: "Все" }, ...JOB_TYPES.map((j) => ({ value: j.id, label: j.label }))];
const regionOptions = computed(() => [
  { value: ANY, label: "Все" },
  ...(props.regions ?? []).map((r) => ({ value: r.id, label: r.displayName })),
]);

const URGENT_OPTIONS = [
  { value: ANY, label: "Все" },
  { value: "true", label: "Срочные" },
  { value: "false", label: "Обычные" },
];
const CREW_OPTIONS = [
  { value: ANY, label: "Все" },
  { value: "false", label: "Назначена" },
  { value: "true", label: "Без бригады" },
];

/** Two-way binding of one filter field through the ANY sentinel */
function field<K extends keyof RequestFilters, C extends string | number>(
  key: K,
  toChoice: (value: NonNullable<RequestFilters[K]>) => C,
  fromChoice: (choice: C) => RequestFilters[K],
) {
  return computed<Choice<C> | null>({
    get: () => {
      const value = filters.value[key];
      return value == null ? ANY : toChoice(value as NonNullable<RequestFilters[K]>);
    },
    set: (choice) => {
      const value = choice == null || choice === ANY ? null : fromChoice(choice as C);
      filters.value = { ...filters.value, [key]: value };
    },
  });
}

const identity = (id: number): number => id;
const status = field("statusId", identity, identity);
const jobType = field("jobTypeId", identity, identity);
const region = field("regionId", identity, identity);
const urgent = field("urgent", String, (c: string) => c === "true");
const crew = field("noAssignedCrew", String, (c: string) => c === "true");
</script>
