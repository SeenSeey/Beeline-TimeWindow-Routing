<template>
  <!-- same page frame as the other pages -->
  <div class="grid grid-cols-[minmax(0,1fr)] content-start gap-4 p-4 sm:gap-6 sm:p-6">
    <header class="grid gap-1">
      <h1 class="m-0 text-2xl font-semibold text-ink">Система</h1>
      <p class="m-0 text-sm text-muted">Служебные действия с расписаниями.</p>
    </header>

    <div class="grid items-stretch gap-4 sm:gap-6 lg:grid-cols-2">
      <SystemActionCard
        title="Сформировать расписание"
        description="Запускает расчёт расписаний на сегодня сразу для всех регионов."
        :effects="[
          'Расписания создаются в статусе «Формируется» и рассчитываются в фоне.',
          'Недоступно, если на сегодня уже есть хотя бы одно расписание.',
        ]"
        action-label="Сформировать расписание"
        running-label="Запускаем расчёт…"
        :result="results.initialize"
        :disabled="busy"
        @run="emit('initialize')"
      >
        <template #icon>
          <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M8 2v4M16 2v4M3 10h18M12 14v4M10 16h4" />
            <rect width="18" height="18" x="3" y="4" rx="2" />
          </svg>
        </template>
      </SystemActionCard>

      <SystemActionCard
        title="Сбросить расписания"
        description="Возвращает систему в исходное состояние для повторного расчёта."
        :effects="[
          'Удаляются все расписания и их слоты.',
          'Все заявки возвращаются в статус «Не назначена», бригады и отметки о выполнении и отмене снимаются.',
          'Очищается журнал действий с заявками.',
        ]"
        action-label="Сбросить расписания"
        running-label="Сбрасываем…"
        :result="results.reset"
        :disabled="busy"
        danger
        confirm-text="Сбросить все расписания? Это действие нельзя отменить."
        confirm-label="Да, сбросить"
        @run="emit('reset')"
      >
        <template #icon>
          <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
            <path d="M3 3v5h5" />
          </svg>
        </template>
      </SystemActionCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { SystemActionId, SystemActionResult } from "../stores/systemActions";
import SystemActionCard from "./SystemActionCard.vue";

defineProps<{
  results: Record<SystemActionId, SystemActionResult>;
  /** an action is running; the others wait */
  busy: boolean;
}>();

const emit = defineEmits<{
  initialize: [];
  reset: [];
}>();
</script>
