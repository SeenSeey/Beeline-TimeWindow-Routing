<template>
  <div class="grid place-items-center p-4 sm:p-6">
    <section
      class="grid w-full max-w-lg gap-4 rounded-lg border border-line bg-surface p-6 shadow-2xs sm:p-7"
      :role="kind === 'loading' ? 'status' : 'alert'"
      :aria-busy="kind === 'loading'"
    >
      <div class="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
        <div class="text-xs font-medium tracking-wide text-muted uppercase">{{ content.eyebrow }}</div>
        <slot name="region" />
      </div>

      <div class="flex items-center gap-x-3">
        <span class="inline-flex size-9 shrink-0 items-center justify-center rounded-full bg-shift">
          <UiDot :color="content.color" :pulse="kind === 'loading'" />
        </span>
        <h1 class="m-0 text-xl font-semibold text-balance text-ink">{{ content.title }}</h1>
      </div>

      <p class="m-0 text-sm text-muted">{{ content.text }}</p>

      <p v-if="message && kind !== 'loading'" class="mono m-0 rounded-lg bg-shift px-3 py-2 text-xs text-ink">
        {{ message }}
      </p>

      <div v-if="kind !== 'loading'" class="flex flex-wrap items-center gap-x-3 gap-y-2">
        <UiButton variant="solid" @click="emit('retry')">Обновить</UiButton>
        <!-- the calculation is started manually on the system page -->
        <RouterLink
          v-if="kind === 'not-found'"
          :to="{ name: 'system' }"
          class="inline-flex h-9.5 items-center rounded-lg border border-line bg-surface px-3 text-sm font-medium text-ink no-underline shadow-2xs hover:bg-shift focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
        >
          Перейти в «Система»
        </RouterLink>
        <span v-if="status" class="mono text-xs text-faint">HTTP {{ status }}</span>
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
    text: "Для этого региона ещё не построено расписание на текущую дату. Запустите расчёт на странице «Система» или обновите страницу позже — после расчёта оно появится здесь.",
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
