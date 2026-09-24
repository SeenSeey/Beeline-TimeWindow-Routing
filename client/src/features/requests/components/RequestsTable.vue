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
        <tr v-for="r in requests" :key="r.id" class="cursor-pointer hover:bg-canvas" @click="openRow($event, r.id)">
          <td class="px-5 py-3 text-sm">
            <RouterLink
              :to="detailsRoute(r.id)"
              class="mono rounded-sm font-medium text-accent no-underline hover:underline focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
            >
              {{ r.id }}
            </RouterLink>
          </td>
          <td class="px-5 py-3 text-sm text-ink">{{ r.fullAddress }}</td>
          <td class="px-5 py-3">
            <UiBadge :color="statusColor(r.status)" dot>{{ statusLabel(r.status) }}</UiBadge>
          </td>
          <td class="px-5 py-3">
            <UiBadge :color="jobTypeColor(r.jobType.keyword)">{{ r.jobType.displayName }}</UiBadge>
          </td>
          <td class="px-5 py-3 text-sm whitespace-nowrap text-muted">{{ r.region.displayName }}</td>
          <td class="px-5 py-3 text-sm whitespace-nowrap text-muted">
            <template v-if="r.assignedCrew">{{ r.assignedCrew.name }}</template>
            <span v-else class="text-faint">—</span>
          </td>
          <td class="px-5 py-3 text-sm">
            <UiBadge v-if="r.urgent" color="var(--urgent)">Срочная</UiBadge>
            <span v-else class="text-faint">—</span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import UiBadge from "@/components/ui/UiBadge.vue";
import { jobTypeColor, statusColor } from "../model/colors";
import { statusLabel } from "../model/dictionaries";
import type { RequestItem } from "../types";

defineProps<{
  requests: RequestItem[];
}>();

/** narrow columns shrink to their content; the address takes the rest */
const COLUMNS = [
  { label: "№", narrow: true },
  { label: "Адрес", narrow: false },
  { label: "Статус", narrow: true },
  { label: "Тип работ", narrow: true },
  { label: "Регион", narrow: true },
  { label: "Бригада", narrow: true },
  { label: "Срочность", narrow: true },
];

const router = useRouter();

const detailsRoute = (id: number) => ({ name: "request", params: { id } });

// The whole row opens the request; the № link keeps it reachable by keyboard and middle-click
function openRow(e: MouseEvent, id: number): void {
  if ((e.target as Element).closest("a") || window.getSelection()?.toString()) return;
  void router.push(detailsRoute(id));
}
</script>
