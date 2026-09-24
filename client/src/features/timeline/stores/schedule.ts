import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { fetchSchedule, toApiFailure, type ApiFailure } from "../api/timelineApi";
import type { ScheduleData } from "../types";

export type ScheduleLoadState = { kind: "loading" } | { kind: "ready"; data: ScheduleData } | ApiFailure;

/** how often a schedule that is still being calculated is refetched */
const CALCULATION_POLL_MS = 3000;

export const useScheduleStore = defineStore("timeline/schedule", () => {
  const state = ref<ScheduleLoadState>({ kind: "loading" });
  /** a region switch is in flight; the current view stays visible meanwhile */
  const switching = ref(false);
  // Only the latest request may update the state (fast region switching)
  let requestSeq = 0;
  // Pending refetch of a schedule that is still CALCULATING
  let pollTimer: ReturnType<typeof setTimeout> | null = null;

  const schedule = computed(() => (state.value.kind === "ready" ? state.value.data : null));

  function stopPolling(): void {
    if (pollTimer != null) clearTimeout(pollTimer);
    pollTimer = null;
  }

  /** Back to the initial loading screen, dropping any request in flight */
  function reset(): void {
    requestSeq++;
    stopPolling();
    switching.value = false;
    state.value = { kind: "loading" };
  }

  function failWith(failure: ApiFailure): void {
    requestSeq++;
    stopPolling();
    switching.value = false;
    state.value = failure;
  }

  const fail = (e: unknown): void => failWith(toApiFailure(e));

  /**
   * Loads the region's schedule. While the server is still calculating it (e.g. right after a manual
   * start on the system page), the store keeps refetching it in the background until it is ready.
   * `background` refetches keep the current view: no busy state, and a failed attempt is simply
   * retried on the next tick.
   */
  async function load(regionId: number, { background = false } = {}): Promise<void> {
    const seq = ++requestSeq;
    stopPolling();
    if (!background) switching.value = state.value.kind !== "loading";

    let next: ScheduleLoadState;
    try {
      const data = await fetchSchedule(regionId);
      next = data ? { kind: "ready", data } : { kind: "not-found", message: null, status: null };
    } catch (e) {
      next = toApiFailure(e);
    }

    if (seq !== requestSeq) return;

    const stillCalculating = next.kind === "ready" && next.data.status.keyword === "CALCULATING";
    const keepCurrentView = background && next.kind !== "ready";
    if (!keepCurrentView) state.value = next;
    switching.value = false;

    if (stillCalculating || keepCurrentView) {
      pollTimer = setTimeout(() => void load(regionId, { background: true }), CALCULATION_POLL_MS);
    }
  }

  /** Shows a schedule the server returned from another call (e.g. after saving an edit) */
  function replace(data: ScheduleData): void {
    requestSeq++;
    stopPolling();
    switching.value = false;
    state.value = { kind: "ready", data };
  }

  return { state, switching, schedule, reset, fail, failWith, load, replace, stopPolling };
});
