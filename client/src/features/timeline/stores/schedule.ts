import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { fetchSchedule, toApiFailure, type ApiFailure } from "../api/timelineApi";
import type { ScheduleData } from "../types";

export type ScheduleLoadState = { kind: "loading" } | { kind: "ready"; data: ScheduleData } | ApiFailure;

export const useScheduleStore = defineStore("timeline/schedule", () => {
  const state = ref<ScheduleLoadState>({ kind: "loading" });
  /** a region switch is in flight; the current view stays visible meanwhile */
  const switching = ref(false);
  // Only the latest request may update the state (fast region switching)
  let requestSeq = 0;

  const schedule = computed(() => (state.value.kind === "ready" ? state.value.data : null));

  /** Back to the initial loading screen, dropping any request in flight */
  function reset(): void {
    requestSeq++;
    switching.value = false;
    state.value = { kind: "loading" };
  }

  function failWith(failure: ApiFailure): void {
    requestSeq++;
    switching.value = false;
    state.value = failure;
  }

  const fail = (e: unknown): void => failWith(toApiFailure(e));

  async function load(regionId: number): Promise<void> {
    const seq = ++requestSeq;
    switching.value = state.value.kind !== "loading";

    let next: ScheduleLoadState;
    try {
      const data = await fetchSchedule(regionId);
      next = data ? { kind: "ready", data } : { kind: "not-found", message: null, status: null };
    } catch (e) {
      next = toApiFailure(e);
    }

    if (seq !== requestSeq) return;
    state.value = next;
    switching.value = false;
  }

  return { state, switching, schedule, reset, fail, failWith, load };
});
