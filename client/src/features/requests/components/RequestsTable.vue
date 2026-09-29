<template>
  <!-- Preline table: gray head with small uppercase labels, divided rows. Scrolls both ways inside
       its container; the head stays pinned (its bottom rule is a shadow, since borders scroll away). -->
  <div class="overflow-auto">
    <table class="min-w-full text-start">
      <thead>
        <tr>
          <!-- every column sorts the whole list on the server: a click cycles ascending → descending → off -->
          <UiSortHeader
            v-for="column in COLUMNS"
            :key="column.label"
            :direction="sort?.field === column.field ? sort.direction : null"
            class="sticky top-0 z-[1] bg-canvas px-5 py-3 text-start text-xs font-semibold tracking-wide whitespace-nowrap text-ink shadow-[inset_0_-1px_0_var(--line)]"
            :class="{ 'w-[1%]': column.narrow }"
            @toggle="emit('sort', column.field)"
          >
            {{ column.label }}
          </UiSortHeader>
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
          <td class="mono px-5 py-3 text-sm whitespace-nowrap text-muted">
            {{ r.createdAt ? formatDateTime(r.createdAt) : "—" }}
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
import UiSortHeader from "@/components/ui/UiSortHeader.vue";
import type { SortOrder } from "@/utils/sort";
import { jobTypeColor, statusColor } from "../model/colors";
import type { RequestSortField } from "../model/query";
import { displayNameOf } from "@/features/dictionaries/model/lookup";
import { useDictionariesStore } from "@/features/dictionaries/stores/dictionaries";
import { formatDateTime } from "../model/format";
import type { RequestItem } from "../types";

// the list carries the status keyword only: its name comes from the server's dictionary (the page loads it)
const dictionaries = useDictionariesStore();
const statusLabel = (keyword: string): string => displayNameOf(dictionaries.requestStatuses, keyword);

defineProps<{
  requests: RequestItem[];
  /** the list's order; null: by number (the server's default) */
  sort: SortOrder<RequestSortField> | null;
}>();

const emit = defineEmits<{
  /** a column head clicked */
  sort: [field: RequestSortField];
}>();

/** narrow columns shrink to their content; the address takes the rest */
const COLUMNS: { label: string; field: RequestSortField; narrow: boolean }[] = [
  { label: "№", field: "id", narrow: true },
  { label: "Создана", field: "createdAt", narrow: true },
  { label: "Адрес", field: "address", narrow: false },
  { label: "Статус", field: "status", narrow: true },
  { label: "Тип работ", field: "jobType", narrow: true },
  { label: "Регион", field: "region", narrow: true },
  { label: "Бригада", field: "crew", narrow: true },
  { label: "Срочность", field: "urgent", narrow: true },
];

const router = useRouter();

const detailsRoute = (id: number) => ({ name: "request", params: { id } });

// The whole row opens the request; the № link keeps it reachable by keyboard and middle-click
function openRow(e: MouseEvent, id: number): void {
  if ((e.target as Element).closest("a") || window.getSelection()?.toString()) return;
  void router.push(detailsRoute(id));
}
</script>
