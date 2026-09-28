<template>
  <UiPanel title="Ход выполнения">
    <template v-if="entries.length" #aside>{{ entries.length }}</template>

    <UiEmptyState v-if="!entries.length">По заявке пока нет записей в журнале.</UiEmptyState>

    <!-- the request's audit as a vertical timeline, oldest first: a coloured marker per action, when it
         happened, and what came with it (the crew, the planned work) -->
    <ol v-else class="m-0 grid list-none gap-0 px-5 py-4">
      <li
        v-for="(entry, index) in entries"
        :key="entry.id"
        class="relative grid grid-cols-[20px_1fr] gap-x-3 pb-5 last:pb-0"
      >
        <!-- connector down to the next entry -->
        <span
          v-if="index < entries.length - 1"
          class="absolute top-5 bottom-0 start-[9.5px] w-px bg-line-strong"
          aria-hidden="true"
        ></span>

        <span
          class="relative z-[1] mt-0.5 inline-flex size-5 items-center justify-center rounded-full"
          :style="{ background: ACTION_STYLES[entry.action]?.color ?? 'var(--muted)' }"
          aria-hidden="true"
        >
          <svg
            class="size-3 text-white"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="3"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path :d="ACTION_STYLES[entry.action]?.icon ?? ICONS.dot" />
          </svg>
        </span>

        <div class="grid min-w-0 gap-0.5">
          <span class="text-sm font-medium text-ink">{{ entry.description }}</span>
          <span class="mono text-xs text-muted">{{ formatDateTime(entry.timestamp) }}</span>

          <span v-if="entry.crewName || entry.plannedStart" class="text-xs text-muted">
            <RouterLink
              v-if="entry.crewId != null && entry.crewName"
              :to="{ name: 'crew', params: { id: entry.crewId } }"
              class="rounded-sm text-accent no-underline hover:underline focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
            >
              {{ entry.crewName }}
            </RouterLink>
            <template v-if="entry.crewName && entry.plannedStart"> · </template>
            <template v-if="entry.plannedStart">
              работа
              <span class="mono">{{ formatTime(entry.plannedStart) }}–{{ formatTime(entry.plannedEnd) }}</span>
            </template>
          </span>
        </div>
      </li>
    </ol>
  </UiPanel>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiEmptyState from "@/components/ui/UiEmptyState.vue";
import UiPanel from "@/components/ui/UiPanel.vue";
import { formatDateTime, formatTime } from "../model/format";
import type { RequestDetailed, RequestHistoryAction } from "../types";

const props = defineProps<{
  request: RequestDetailed;
}>();

/** 24×24 icon paths, drawn white on the marker */
const ICONS = {
  plus: "M12 5v14M5 12h14",
  user: "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z",
  swap: "M7 16V4m0 0L3 8m4-4 4 4M17 8v12m0 0 4-4m-4 4-4-4",
  clock: "M12 7v5l3 2M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z",
  minus: "M5 12h14",
  play: "M8 5v14l11-7z",
  check: "M20 6 9 17l-5-5",
  cross: "M18 6 6 18M6 6l12 12",
  dot: "M12 12h.01",
};

/** the marker of each action: what happened, in the page's status colours */
const ACTION_STYLES: Record<RequestHistoryAction, { color: string; icon: string }> = {
  REQUEST_CREATED: { color: "var(--accent)", icon: ICONS.plus },
  ASSIGNED: { color: "var(--accent)", icon: ICONS.user },
  REASSIGNED: { color: "var(--accent)", icon: ICONS.swap },
  RESCHEDULED: { color: "var(--job-addon)", icon: ICONS.clock },
  REMOVED_FROM_SCHEDULE: { color: "var(--unassigned)", icon: ICONS.minus },
  STARTED: { color: "var(--accent)", icon: ICONS.play },
  COMPLETED: { color: "var(--job-local)", icon: ICONS.check },
  CANCELED: { color: "var(--unassigned)", icon: ICONS.cross },
};

// the server sends the newest first; the timeline reads from the start. `?? []`: a server from before the
// audit was added to the request doesn't send it
const entries = computed(() => [...(props.request.history ?? [])].reverse());
</script>
