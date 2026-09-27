import { defineStore } from "pinia";
import { ref } from "vue";
import { fetchCrew, takeCrewOffShift, toApiFailure, type ApiFailure } from "../api/crewsApi";
import type { CrewDetailed } from "../types";

export type CrewDetailsLoadState = { kind: "loading" } | { kind: "ready"; data: CrewDetailed } | ApiFailure;

export const useCrewDetailsStore = defineStore("crews/details", () => {
  const state = ref<CrewDetailsLoadState>({ kind: "loading" });
  /** taking the shown crew off shift is in flight */
  const offShiftPending = ref(false);
  /** the shown crew was taken off shift from this page */
  const offShiftDone = ref(false);
  /** why the last off-shift attempt failed; cleared when another crew is shown or on the next try */
  const offShiftError = ref<string | null>(null);
  // Only the latest request may update the state (quick back/forward between crews)
  let requestSeq = 0;

  async function load(id: number): Promise<void> {
    const seq = ++requestSeq;
    // a different crew never shows the previous one's data (or its off-shift outcome) meanwhile
    if (state.value.kind !== "ready" || state.value.data.id !== id) {
      state.value = { kind: "loading" };
      offShiftDone.value = false;
      offShiftError.value = null;
    }

    let next: CrewDetailsLoadState;
    try {
      const data = await fetchCrew(id);
      next = data ? { kind: "ready", data } : { kind: "not-found", message: null, status: null };
    } catch (e) {
      next = toApiFailure(e);
    }
    if (seq === requestSeq) state.value = next;
  }

  /** Takes the crew off today's shift, then reloads it so the page shows the recalculated route */
  async function takeOffShift(id: number): Promise<void> {
    if (offShiftPending.value) return;
    offShiftPending.value = true;
    offShiftError.value = null;
    try {
      await takeCrewOffShift(id);
      offShiftDone.value = true;
    } catch (e) {
      const failure = toApiFailure(e);
      offShiftError.value =
        failure.message ?? `Не удалось снять бригаду со смены${failure.status ? ` (HTTP ${failure.status})` : ""}`;
    } finally {
      offShiftPending.value = false;
    }
    await load(id);
  }

  return { state, offShiftPending, offShiftDone, offShiftError, load, takeOffShift };
});
