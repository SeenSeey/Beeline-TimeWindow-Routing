<template>
  <div class="overflow-x-auto">
    <table class="w-full border-collapse text-left">
      <thead>
        <tr class="border-b border-line text-[11px] font-medium tracking-[0.08em] text-muted uppercase">
          <th scope="col" class="w-[1%] px-4 py-2 font-medium whitespace-nowrap">№</th>
          <th scope="col" class="px-4 py-2 font-medium">Адрес</th>
          <th scope="col" class="w-[1%] px-4 py-2 font-medium whitespace-nowrap">Статус</th>
          <th scope="col" class="w-[1%] px-4 py-2 font-medium whitespace-nowrap">Тип работ</th>
          <th scope="col" class="w-[1%] px-4 py-2 font-medium whitespace-nowrap">Регион</th>
          <th scope="col" class="w-[1%] px-4 py-2 font-medium whitespace-nowrap">Бригада</th>
          <th scope="col" class="w-[1%] px-4 py-2 font-medium whitespace-nowrap">Срочность</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="r in requests"
          :key="r.id"
          class="cursor-pointer border-b border-line last:border-b-0 hover:bg-shift"
          @click="openRow($event, r.id)"
        >
          <td class="px-4 py-2.5">
            <RouterLink
              :to="detailsRoute(r.id)"
              class="mono text-accent no-underline hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
            >
              {{ r.id }}
            </RouterLink>
          </td>
          <td class="px-4 py-2.5">{{ r.fullAddress }}</td>
          <td class="px-4 py-2.5">
            <UiBadge :color="statusColor(r.status)" dot>{{ statusLabel(r.status) }}</UiBadge>
          </td>
          <td class="px-4 py-2.5">
            <UiBadge :color="jobTypeColor(r.jobType.keyword)">{{ r.jobType.displayName }}</UiBadge>
          </td>
          <td class="px-4 py-2.5 whitespace-nowrap">{{ r.region.displayName }}</td>
          <td class="px-4 py-2.5 whitespace-nowrap">
            <template v-if="r.assignedCrew">{{ r.assignedCrew.name }}</template>
            <span v-else class="text-faint">—</span>
          </td>
          <td class="px-4 py-2.5">
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

const router = useRouter();

const detailsRoute = (id: number) => ({ name: "request", params: { id } });

// The whole row opens the request; the № link keeps it reachable by keyboard and middle-click
function openRow(e: MouseEvent, id: number): void {
  if ((e.target as Element).closest("a") || window.getSelection()?.toString()) return;
  void router.push(detailsRoute(id));
}
</script>
