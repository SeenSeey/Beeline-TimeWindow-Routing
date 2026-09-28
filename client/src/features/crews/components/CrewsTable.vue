<template>
  <!-- Preline table: gray head with small uppercase labels, divided rows. Scrolls both ways inside
       its container; the head stays pinned (its bottom rule is a shadow, since borders scroll away). -->
  <div class="overflow-auto">
    <table class="min-w-full text-start">
      <thead>
        <tr>
          <!-- the sortable columns sort the whole list on the server: a click cycles ascending → descending → off;
               the skills are a list and don't sort -->
          <template v-for="column in COLUMNS" :key="column.label">
            <UiSortHeader
              v-if="column.field"
              :direction="sort?.field === column.field ? sort.direction : null"
              :class="[HEAD_CLASS, { 'w-[1%]': column.narrow }]"
              @toggle="emit('sort', column.field!)"
            >
              {{ column.label }}
            </UiSortHeader>
            <th v-else scope="col" class="uppercase" :class="[HEAD_CLASS, { 'w-[1%]': column.narrow }]">
              {{ column.label }}
            </th>
          </template>
        </tr>
      </thead>
      <tbody class="divide-y divide-line">
        <tr v-for="crew in crews" :key="crew.id" class="cursor-pointer hover:bg-canvas" @click="openRow($event, crew.id)">
          <td class="px-5 py-3 text-sm">
            <RouterLink
              :to="detailsRoute(crew.id)"
              class="mono rounded-sm font-medium text-accent no-underline hover:underline focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
            >
              {{ crew.id }}
            </RouterLink>
          </td>
          <td class="px-5 py-3 text-sm font-medium whitespace-nowrap text-ink">{{ crew.name }}</td>
          <td class="px-5 py-3 text-sm whitespace-nowrap text-muted">{{ crew.regionName }}</td>
          <td class="px-5 py-3 text-sm whitespace-nowrap text-muted">
            <span class="inline-flex items-center gap-x-2">
              <VehicleIcon :vehicle="{ keyword: crew.vehicle, displayName: vehicleLabel(crew.vehicle) }" class="size-4" />
              {{ vehicleLabel(crew.vehicle) }}
            </span>
          </td>
          <td class="px-5 py-3">
            <div v-if="crew.skills.length" class="flex flex-wrap gap-1.5">
              <UiBadge v-for="skill in crew.skills" :key="skill" :color="SKILL_COLORS[skill] ?? 'var(--muted)'">
                {{ skillLabel(skill) }}
              </UiBadge>
            </div>
            <span v-else class="text-sm text-faint">—</span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import UiBadge from "@/components/ui/UiBadge.vue";
import UiSortHeader from "@/components/ui/UiSortHeader.vue";
import VehicleIcon from "@/features/timeline/components/VehicleIcon.vue";
import type { SortOrder } from "@/utils/sort";
import { skillLabel, vehicleLabel } from "../model/dictionaries";
import type { CrewSortField } from "../model/query";
import type { CrewItem, SkillKeyword } from "../types";

defineProps<{
  crews: CrewItem[];
  /** the list's order; null: by number (the server's default) */
  sort: SortOrder<CrewSortField> | null;
}>();

const emit = defineEmits<{
  /** a sortable column head clicked */
  sort: [field: CrewSortField];
}>();

/** the look of a column head: pinned on scroll, small uppercase-style labels */
const HEAD_CLASS =
  "sticky top-0 z-[1] bg-canvas px-5 py-3 text-start text-xs font-semibold tracking-wide whitespace-nowrap text-ink shadow-[inset_0_-1px_0_var(--line)]";

const router = useRouter();

const detailsRoute = (id: number) => ({ name: "crew", params: { id } });

// The whole row opens the crew; the № link keeps it reachable by keyboard and middle-click
function openRow(e: MouseEvent, id: number): void {
  if ((e.target as Element).closest("a") || window.getSelection()?.toString()) return;
  void router.push(detailsRoute(id));
}

/** narrow columns shrink to their content; the skills take the rest. `field`: what the column sorts by */
const COLUMNS: { label: string; field: CrewSortField | null; narrow: boolean }[] = [
  { label: "№", field: "id", narrow: true },
  { label: "Бригада", field: "name", narrow: true },
  { label: "Регион", field: "region", narrow: true },
  { label: "Транспорт", field: "vehicle", narrow: true },
  { label: "Навыки", field: null, narrow: false },
];

/** each skill in the colour of the job types it covers */
const SKILL_COLORS: Record<SkillKeyword, string> = {
  LOCAL_WORK: "var(--job-local)",
  INSTALLATION_OR_ADDON: "var(--job-installation)",
  EMERGENCY: "var(--job-emergency)",
};
</script>
