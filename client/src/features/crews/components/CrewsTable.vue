<template>
  <!-- Preline table: gray head with small uppercase labels, divided rows. Scrolls both ways inside
       its container; the head stays pinned (its bottom rule is a shadow, since borders scroll away). -->
  <div class="overflow-auto">
    <table class="min-w-full text-start">
      <thead>
        <tr>
          <th
            v-for="column in COLUMNS"
            :key="column.label"
            scope="col"
            class="sticky top-0 z-[1] bg-canvas px-5 py-3 text-start text-xs font-semibold tracking-wide whitespace-nowrap text-ink uppercase shadow-[inset_0_-1px_0_var(--line)]"
            :class="{ 'w-[1%]': column.narrow }"
          >
            {{ column.label }}
          </th>
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
import VehicleIcon from "@/features/timeline/components/VehicleIcon.vue";
import { skillLabel, vehicleLabel } from "../model/dictionaries";
import type { CrewItem, SkillKeyword } from "../types";

defineProps<{
  crews: CrewItem[];
}>();

const router = useRouter();

const detailsRoute = (id: number) => ({ name: "crew", params: { id } });

// The whole row opens the crew; the № link keeps it reachable by keyboard and middle-click
function openRow(e: MouseEvent, id: number): void {
  if ((e.target as Element).closest("a") || window.getSelection()?.toString()) return;
  void router.push(detailsRoute(id));
}

/** narrow columns shrink to their content; the skills take the rest */
const COLUMNS = [
  { label: "№", narrow: true },
  { label: "Бригада", narrow: true },
  { label: "Регион", narrow: true },
  { label: "Транспорт", narrow: true },
  { label: "Навыки", narrow: false },
];

/** each skill in the colour of the job types it covers */
const SKILL_COLORS: Record<SkillKeyword, string> = {
  LOCAL_WORK: "var(--job-local)",
  INSTALLATION_OR_ADDON: "var(--job-installation)",
  EMERGENCY: "var(--job-emergency)",
};
</script>
