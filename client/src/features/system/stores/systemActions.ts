import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { initializeSchedules, resetSchedules, toApiFailure } from "../api/systemApi";

export type SystemActionId = "initialize" | "reset";

/** Outcome of the last run of an action, shown under its button */
export type SystemActionResult =
  | { kind: "idle" }
  | { kind: "running" }
  | { kind: "success"; message: string; finishedAt: Date }
  | { kind: "error"; message: string; finishedAt: Date };

function failureMessage(e: unknown, fallback: string): string {
  const failure = toApiFailure(e);
  return failure.message ?? `${fallback}${failure.status ? ` (HTTP ${failure.status})` : ""}`;
}

export const useSystemActionsStore = defineStore("system/actions", () => {
  const results = ref<Record<SystemActionId, SystemActionResult>>({
    initialize: { kind: "idle" },
    reset: { kind: "idle" },
  });

  /** both actions change the same schedules, so only one may run at a time */
  const busy = computed(() => Object.values(results.value).some((result) => result.kind === "running"));

  async function run(id: SystemActionId, action: () => Promise<string>, fallbackError: string): Promise<void> {
    if (busy.value) return;
    results.value[id] = { kind: "running" };
    try {
      const message = await action();
      results.value[id] = { kind: "success", message, finishedAt: new Date() };
    } catch (e) {
      results.value[id] = { kind: "error", message: failureMessage(e, fallbackError), finishedAt: new Date() };
    }
  }

  const initialize = (): Promise<void> =>
    run(
      "initialize",
      async () => {
        const createdCount = await initializeSchedules();
        return `Расчёт запущен: создано расписаний — ${createdCount}. Они появятся на странице «Расписание» после расчёта.`;
      },
      "Не удалось запустить расчёт",
    );

  const reset = (): Promise<void> =>
    run(
      "reset",
      async () => {
        await resetSchedules();
        return "Расписания удалены, все заявки возвращены в статус «Не назначена».";
      },
      "Не удалось сбросить расписания",
    );

  return { results, busy, initialize, reset };
});
