import { defineStore } from "pinia";
import { ref } from "vue";
import { fetchRequestPoints, toApiFailure, type ApiFailure } from "../api/mapApi";
import type { RequestPoint } from "../types";

export type RequestPointsLoadState = { kind: "loading" } | { kind: "ready"; points: RequestPoint[] } | ApiFailure;

export const useRequestPointsStore = defineStore("map/requestPoints", () => {
  const state = ref<RequestPointsLoadState>({ kind: "loading" });
  /** a region switch is in flight; the current points stay on the map meanwhile */
  const switching = ref(false);
  // Only the latest request may update the state (fast region switching)
  let requestSeq = 0;

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
    switching.value = state.value.kind === "ready";

    let next: RequestPointsLoadState;
    try {
      next = { kind: "ready", points: await fetchRequestPoints(regionId) };
    } catch (e) {
      next = toApiFailure(e);
    }

    if (seq !== requestSeq) return;
    state.value = next;
    switching.value = false;
  }

  return { state, switching, reset, fail, failWith, load };
});
