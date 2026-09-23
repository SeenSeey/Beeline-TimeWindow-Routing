<template>
  <div
    class="board grid min-h-screen place-items-center bg-(--bg) px-5 py-10 font-(family-name:--sans) text-sm leading-[1.45] text-(--ink)"
  >
    <section
      class="grid w-full max-w-[520px] gap-3 rounded-lg border border-(--line) bg-(--surface) px-6 py-7"
      :role="kind === 'loading' ? 'status' : 'alert'"
      :aria-busy="kind === 'loading'"
    >
      <div class="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
        <div class="text-[11px] font-medium tracking-[0.08em] text-(--muted) uppercase">{{ content.eyebrow }}</div>
        <slot name="region" />
      </div>

      <div class="flex items-center gap-2.5">
        <span
          class="inline-block h-[9px] w-[9px] flex-none rounded-full"
          :class="kind === 'loading' && 'motion-safe:animate-pulse'"
          :style="{ background: content.color }"
        ></span>
        <h1 class="m-0 text-[22px] font-semibold tracking-[-0.01em] text-balance">{{ content.title }}</h1>
      </div>

      <p class="m-0 text-(--muted)">{{ content.text }}</p>

      <p
        v-if="message && kind !== 'loading'"
        class="m-0 rounded-md bg-(--shift) px-3 py-2 font-(family-name:--mono) text-[12.5px] text-(--ink)"
      >
        {{ message }}
      </p>

      <div v-if="kind !== 'loading'" class="flex flex-wrap items-center gap-x-4 gap-y-2 pt-1">
        <button
          type="button"
          class="inline-flex cursor-pointer items-center gap-1.5 rounded-full border border-(--ink) bg-(--ink) px-[11px] py-1 text-[13px] text-(--bg) focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-(--accent)"
          @click="emit('retry')"
        >
          Обновить
        </button>
        <span v-if="status" class="font-(family-name:--mono) text-[12px] text-(--faint)">HTTP {{ status }}</span>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  kind: "loading" | "not-found" | "error";
  /** message from the server's error response, shown as-is */
  message?: string | null;
  status?: number | null;
}>();

const emit = defineEmits<{
  retry: [];
}>();

const CONTENT = {
  loading: {
    eyebrow: "Доска выездов",
    title: "Загружаем расписание…",
    text: "Получаем актуальное расписание бригад для региона.",
    color: "var(--faint)",
  },
  "not-found": {
    eyebrow: "Расписание не найдено",
    title: "Расписания пока нет",
    text: "Для этого региона ещё не построено расписание на текущую дату. Попробуйте обновить страницу позже — после расчёта оно появится здесь.",
    color: "var(--job-addon)",
  },
  error: {
    eyebrow: "Ошибка",
    title: "Не удалось загрузить расписание",
    text: "Сервер не ответил или вернул ошибку. Проверьте соединение и попробуйте ещё раз.",
    color: "var(--unassigned)",
  },
} as const;

const content = computed(() => CONTENT[props.kind]);
</script>
