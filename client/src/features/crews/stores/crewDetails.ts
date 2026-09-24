import { defineStore } from "pinia";
import { ref } from "vue";
import { fetchCrew, toApiFailure, type ApiFailure } from "../api/crewsApi";
import type { CrewDetailed } from "../types";

export type CrewDetailsLoadState = { kind: "loading" } | { kind: "ready"; data: CrewDetailed } | ApiFailure;

export const useCrewDetailsStore = defineStore("crews/details", () => {
  const state = ref<CrewDetailsLoadState>({ kind: "loading" });
  // Only the latest request may update the state (quick back/forward between crews)
  let requestSeq = 0;

  async function load(id: number): Promise<void> {
    const seq = ++requestSeq;
    // a different crew never shows the previous one's data meanwhile
    if (state.value.kind !== "ready" || state.value.data.id !== id) state.value = { kind: "loading" };

    let next: CrewDetailsLoadState;
    try {
      const data = await fetchCrew(id);
      next = data ? { kind: "ready", data } : { kind: "not-found", message: null, status: null };
    } catch (e) {
      next = toApiFailure(e);
    }
    if (seq === requestSeq) state.value = next;
  }

  return { state, load };
});
