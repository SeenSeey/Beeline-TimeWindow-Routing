<template>
  <div class="grid place-items-center p-4 sm:p-6">
    <section
      class="grid w-full max-w-lg gap-4 rounded-lg border border-line bg-surface p-6 shadow-2xs sm:p-7"
      :role="kind === 'loading' ? 'status' : 'alert'"
      :aria-busy="kind === 'loading'"
    >
      <div class="text-xs font-medium tracking-wide text-muted uppercase">Бригада №{{ id }}</div>

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

      <div v-if="kind !== 'loading'" class="flex flex-wrap items-center gap-x-4 gap-y-2">
        <UiButton v-if="kind === 'error'" variant="solid" @click="emit('retry')">Обновить</UiButton>
        <RouterLink
          :to="{ name: 'crews' }"
          class="rounded-sm text-sm font-medium text-accent no-underline hover:underline focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
        >
          К списку исполнителей
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
  id: number;
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
    title: "Загружаем бригаду…",
    text: "Получаем данные о бригаде и её заявках.",
    color: "var(--faint)",
  },
  "not-found": {
    title: "Бригада не найдена",
    text: "Такой бригады нет — возможно, ссылка устарела.",
    color: "var(--job-addon)",
  },
  error: {
    title: "Не удалось загрузить бригаду",
    text: "Сервер не ответил или вернул ошибку. Проверьте соединение и попробуйте ещё раз.",
    color: "var(--unassigned)",
  },
} as const;

const content = computed(() => CONTENT[props.kind]);
</script>
