<template>
  <UiSelect v-model="regionId" label="Регион" :options="options" :busy="busy" />
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiSelect from "@/components/ui/UiSelect.vue";
import type { RegionInfo } from "../types";

const props = defineProps<{
  regions: RegionInfo[];
  /** the chosen region's schedule is loading */
  busy?: boolean;
}>();

const regionId = defineModel<number | null>({ required: true });

const options = computed(() => props.regions.map((r) => ({ value: r.id, label: r.displayName })));
</script>
