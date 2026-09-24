<template>
  <!-- the edit mode's status, its problems and actions; after a save it stays to report the result -->
  <section
    v-if="edit.active || edit.notice"
    class="grid gap-3 rounded-lg border p-4 shadow-2xs"
    :class="edit.active ? 'border-accent/50 bg-accent-soft' : 'border-line bg-surface'"
    aria-live="polite"
  >
    <div v-if="edit.active" class="flex flex-wrap items-center justify-between gap-x-4 gap-y-3">
      <div class="grid gap-0.5">
        <div class="flex flex-wrap items-center gap-x-2.5">
          <UiDot color="var(--accent)" :pulse="!!edit.busy" />
          <span class="text-sm font-semibold text-ink">Режим редактирования</span>
          <UiBadge v-if="edit.changes.length" color="var(--accent)">
            изменено бригад: <span class="mono">{{ edit.changes.length }}</span>
          </UiBadge>
          <span v-if="edit.busy === 'previewing'" class="text-xs text-muted">Проверяем изменения…</span>
          <span v-else-if="edit.busy === 'saving'" class="text-xs text-muted">Сохраняем…</span>
        </div>
        <p class="m-0 text-xs text-muted">
          Перетащите заявку на строку бригады в нужное время — туда же можно перетащить заявку из списка
          «Не вошли в расписание». Нажмите на заявку, чтобы убрать её из расписания или переместить через меню.
        </p>
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <UiButton v-if="edit.changes.length" :disabled="!!edit.busy" @click="edit.discardChanges()">
          Сбросить изменения
        </UiButton>
        <UiButton :disabled="edit.busy === 'saving'" @click="edit.stop()">Отменить</UiButton>
        <UiButton
          variant="solid"
          :disabled="!edit.canSave"
          :title="saveBlockedReason ?? undefined"
          :aria-describedby="saveBlockedReason ? problemsId : undefined"
          @click="edit.save()"
        >
          <UiDot v-if="edit.busy === 'saving'" size="sm" pulse />
          Сохранить
        </UiButton>
      </div>
    </div>

    <!-- what the draft breaks: saving stays blocked until it is fixed -->
    <div
      v-if="edit.active && problems.length"
      :id="problemsId"
      class="grid gap-1.5 rounded-md bg-danger/10 px-3 py-2 text-sm text-danger"
      role="alert"
    >
      <span class="font-medium">Сохранение недоступно — исправьте расписание:</span>
      <ul class="m-0 grid list-disc gap-1 ps-5 text-xs">
        <li v-for="problem in problems" :key="problem.requestId">
          <span class="mono font-semibold">Заявка #{{ problem.requestId }}</span>
          <span class="text-muted"> · {{ problem.crewName }}</span>:
          {{ problem.messages.join("; ") }}
        </li>
      </ul>
    </div>

    <div
      v-if="edit.notice"
      class="flex items-start justify-between gap-3 rounded-md px-3 py-2 text-sm"
      :class="edit.notice.kind === 'error' ? 'bg-danger/10 text-danger' : 'bg-current/10 text-[var(--job-local)]'"
      :role="edit.notice.kind === 'error' ? 'alert' : 'status'"
    >
      <span class="font-medium">{{ edit.notice.message }}</span>
      <button
        type="button"
        class="shrink-0 cursor-pointer rounded-sm p-0.5 opacity-70 hover:opacity-100 focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
        aria-label="Скрыть сообщение"
        @click="edit.dismissNotice()"
      >
        <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <path d="M18 6 6 18M6 6l12 12" />
        </svg>
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, useId } from "vue";
import UiBadge from "@/components/ui/UiBadge.vue";
import UiButton from "@/components/ui/UiButton.vue";
import UiDot from "@/components/ui/UiDot.vue";
import { useBoardStore } from "../stores/board";
import { useScheduleEditStore } from "../stores/scheduleEdit";

const board = useBoardStore();
const edit = useScheduleEditStore();
const problemsId = useId();

/** the violations grouped by request, with the crew it is on */
const problems = computed(() => {
  const crewNames = new Map(board.crews.map((crew) => [crew.id, crew.name]));
  const byRequest = new Map<number, { requestId: number; crewName: string; messages: string[] }>();
  for (const violation of edit.violations) {
    const problem = byRequest.get(violation.requestId) ?? {
      requestId: violation.requestId,
      crewName: crewNames.get(violation.crewId) ?? `бригада №${violation.crewId}`,
      messages: [],
    };
    problem.messages.push(violation.message);
    byRequest.set(violation.requestId, problem);
  }
  return [...byRequest.values()];
});

const saveBlockedReason = computed(() => {
  if (edit.canSave) return null;
  if (edit.busy === "previewing") return "Дождитесь окончания проверки";
  if (!edit.changes.length) return "Изменений нет";
  if (problems.value.length) return "В расписании есть ошибки";
  return null;
});
</script>
