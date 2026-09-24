<template>
  <!-- popover card -->
  <article class="grid gap-3 rounded-lg border border-line bg-surface p-4 text-sm shadow-lg">
    <div class="flex items-start justify-between gap-3">
      <RouterLink
        :to="{ name: 'request', params: { id: point.id } }"
        class="rounded-sm text-base font-semibold text-ink no-underline hover:text-accent focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
      >
        Заявка <span class="mono">№{{ point.id }}</span>
      </RouterLink>
      <UiButton size="icon" aria-label="Закрыть" @click="emit('close')">
        <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <path d="M18 6 6 18M6 6l12 12" />
        </svg>
      </UiButton>
    </div>

    <p class="m-0 text-muted">{{ point.fullAddress }}</p>

    <div class="flex flex-wrap gap-1.5">
      <UiBadge :color="jobTypeColor(point.jobType.keyword)">{{ point.jobType.displayName }}</UiBadge>
      <UiBadge v-if="point.status">{{ point.status.displayName }}</UiBadge>
      <UiBadge v-if="point.urgent" color="var(--urgent)">срочная</UiBadge>
    </div>

    <dl class="m-0 grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-xs">
      <dt class="text-muted">Окно</dt>
      <dd class="mono m-0 text-ink">
        {{ formatTime(point.requestTimeWindowStart) }}–{{ formatTime(point.requestTimeWindowEnd) }}
      </dd>
      <dt class="text-muted">Срок</dt>
      <dd class="mono m-0 text-ink">{{ formatDateTime(point.deadline) }}</dd>
      <dt class="text-muted">Бригада</dt>
      <dd class="m-0" :class="point.assignedCrew ? 'text-ink' : 'text-faint'">
        {{ point.assignedCrew ?? "не назначена" }}
      </dd>
    </dl>

    <RouterLink
      :to="{ name: 'request', params: { id: point.id } }"
      class="inline-flex h-9.5 items-center justify-center gap-x-2 rounded-lg bg-accent px-3 text-sm font-medium text-white no-underline hover:bg-accent-hover focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
    >
      Подробнее
    </RouterLink>
  </article>
</template>

<script setup lang="ts">
import UiBadge from "@/components/ui/UiBadge.vue";
import UiButton from "@/components/ui/UiButton.vue";
import { formatDateTime, formatTime } from "@/features/timeline/model/format";
import { jobTypeColor } from "../model/points";
import type { RequestPoint } from "../types";

defineProps<{
  point: RequestPoint;
}>();

const emit = defineEmits<{
  close: [];
}>();
</script>
