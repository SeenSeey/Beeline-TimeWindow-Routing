import { defineStore } from "pinia";
import { ref } from "vue";
import { fetchRequest, toApiFailure, type ApiFailure } from "../api/requestsApi";
import type { RequestDetailed } from "../types";

export type RequestDetailsLoadState = { kind: "loading" } | { kind: "ready"; data: RequestDetailed } | ApiFailure;

export const useRequestDetailsStore = defineStore("requests/details", () => {
  const state = ref<RequestDetailsLoadState>({ kind: "loading" });
  // Only the latest request may update the state (quick back/forward between requests)
  let requestSeq = 0;

  async function load(id: number): Promise<void> {
    const seq = ++requestSeq;
    // a different request never shows the previous one's data meanwhile
    if (state.value.kind !== "ready" || state.value.data.id !== id) state.value = { kind: "loading" };

    let next: RequestDetailsLoadState;
    try {
      const data = await fetchRequest(id);
      next = data ? { kind: "ready", data } : { kind: "not-found", message: null, status: null };
    } catch (e) {
      next = toApiFailure(e);
    }
    if (seq === requestSeq) state.value = next;
  }

  return { state, load };
});
