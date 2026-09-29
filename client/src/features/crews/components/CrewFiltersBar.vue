<template>
  <!-- one sideways-scrolling row on phones, wrapping from sm up (same as the requests filters) -->
  <div
    class="-my-1 flex items-center gap-x-4 gap-y-2.5 overflow-x-auto py-1 *:shrink-0 sm:flex-wrap sm:overflow-visible"
  >
    <UiInput v-model="nameInput" label="Бригада" search placeholder="Название" />
    <UiSelect v-model="region" label="Регион" :options="regionOptions" />
    <UiSelect v-model="vehicle" label="Транспорт" :options="vehicleOptions" />
    <UiSelect v-model="skill" label="Навык" :options="skillOptions" />
    <UiButton v-if="hasActiveFilters(filters)" @click="emit('reset')">Сбросить</UiButton>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";
import UiButton from "@/components/ui/UiButton.vue";
import UiInput from "@/components/ui/UiInput.vue";
import UiSelect from "@/components/ui/UiSelect.vue";
import { toOptions } from "@/features/dictionaries/model/lookup";
import { useDictionariesStore } from "@/features/dictionaries/stores/dictionaries";
import { hasActiveFilters } from "../model/query";
import type { CrewFilters, RegionInfo } from "../types";

const props = defineProps<{
  regions: RegionInfo[];
}>();

const filters = defineModel<CrewFilters>("filters", { required: true });

const emit = defineEmits<{
  reset: [];
}>();

// ── Name search: the field updates at once, the filter only after a typing pause ──
const NAME_DEBOUNCE_MS = 300;
const nameInput = ref(filters.value.name ?? "");
let nameTimer: ReturnType<typeof setTimeout> | null = null;

watch(nameInput, (text) => {
  if (nameTimer != null) clearTimeout(nameTimer);
  nameTimer = setTimeout(() => {
    const name = text.trim() || null;
    if (name !== filters.value.name) filters.value = { ...filters.value, name };
  }, NAME_DEBOUNCE_MS);
});

// reset (or a URL change) from outside: show the new value in the field
watch(
  () => filters.value.name,
  (name) => {
    if ((nameInput.value.trim() || null) !== name) nameInput.value = name ?? "";
  },
);

onBeforeUnmount(() => {
  if (nameTimer != null) clearTimeout(nameTimer);
});

// ── Selects: UiSelect needs a real option for "no filter"; `ANY` stands for null ──
const ANY = "any" as const;
type IdFilterKey = "regionId" | "vehicleId" | "skillId";

const withAny = (options: { value: number; label: string }[]) => [{ value: ANY, label: "Все" }, ...options];

// the vehicles and skills as the server has them (the page loads the dictionaries)
const dictionaries = useDictionariesStore();

const regionOptions = computed(() => withAny(props.regions.map((r) => ({ value: r.id, label: r.displayName }))));
const vehicleOptions = computed(() => withAny(toOptions(dictionaries.vehicles)));
const skillOptions = computed(() => withAny(toOptions(dictionaries.skills)));

/** Two-way binding of one id filter through the ANY sentinel */
function idField(key: IdFilterKey) {
  return computed<number | typeof ANY | null>({
    get: () => filters.value[key] ?? ANY,
    set: (choice) => {
      filters.value = { ...filters.value, [key]: choice == null || choice === ANY ? null : choice };
    },
  });
}

const region = idField("regionId");
const vehicle = idField("vehicleId");
const skill = idField("skillId");
</script>
