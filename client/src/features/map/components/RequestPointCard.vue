<template>
  <article class="grid gap-2.5 rounded-lg border border-line bg-surface px-4 py-3.5 shadow-[0_6px_20px_rgb(0_0_0/0.14)]">
    <div class="flex items-start justify-between gap-3">
      <RouterLink
        :to="{ name: 'request', params: { id: point.id } }"
        class="font-semibold text-ink no-underline hover:text-accent hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
      >
        Заявка <span class="mono">№{{ point.id }}</span>
      </RouterLink>
      <UiButton size="icon" aria-label="Закрыть" @click="emit('close')">✕</UiButton>
    </div>

    <p class="m-0">{{ point.fullAddress }}</p>

    <div class="flex flex-wrap gap-1.5">
      <UiBadge :color="jobTypeColor(point.jobType.keyword)">{{ point.jobType.displayName }}</UiBadge>
      <UiBadge v-if="point.status">{{ point.status.displayName }}</UiBadge>
      <UiBadge v-if="point.urgent" color="var(--urgent)">срочная</UiBadge>
    </div>

    <dl class="m-0 grid grid-cols-[auto_1fr] gap-x-3 gap-y-0.5 text-[12.5px]">
      <dt class="text-muted">Окно</dt>
      <dd class="mono m-0">
        {{ formatTime(point.requestTimeWindowStart) }}–{{ formatTime(point.requestTimeWindowEnd) }}
      </dd>
      <dt class="text-muted">Срок</dt>
      <dd class="mono m-0">{{ formatDateTime(point.deadline) }}</dd>
      <dt class="text-muted">Бригада</dt>
      <dd class="m-0" :class="{ 'text-faint': !point.assignedCrew }">{{ point.assignedCrew ?? "не назначена" }}</dd>
    </dl>

    <RouterLink
      :to="{ name: 'request', params: { id: point.id } }"
      class="w-fit text-[13px] text-accent no-underline hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
    >
      Подробнее →
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
