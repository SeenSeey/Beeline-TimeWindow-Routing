<template>
  <div class="grid place-items-center px-5 py-10">
    <section
      class="grid w-full max-w-[520px] gap-3 rounded-lg border border-line bg-surface px-6 py-7"
      :role="kind === 'loading' ? 'status' : 'alert'"
      :aria-busy="kind === 'loading'"
    >
      <div class="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
        <div class="text-[11px] font-medium tracking-[0.08em] text-muted uppercase">{{ content.eyebrow }}</div>
        <slot name="region" />
      </div>

      <div class="flex items-center gap-2.5">
        <UiDot :color="content.color" :pulse="kind === 'loading'" />
        <h1 class="m-0 text-[22px] font-semibold tracking-[-0.01em] text-balance">{{ content.title }}</h1>
      </div>

      <p class="m-0 text-muted">{{ content.text }}</p>

      <p v-if="message && kind !== 'loading'" class="mono m-0 rounded-md bg-shift px-3 py-2 text-[12.5px] text-ink">
        {{ message }}
      </p>

      <div v-if="kind !== 'loading'" class="flex flex-wrap items-center gap-x-4 gap-y-2 pt-1">
        <UiButton variant="solid" @click="emit('retry')">Обновить</UiButton>
        <span v-if="status" class="mono text-[12px] text-faint">HTTP {{ status }}</span>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiButton from "@/components/ui/UiButton.vue";
import UiDot from "@/components/ui/UiDot.vue";

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
