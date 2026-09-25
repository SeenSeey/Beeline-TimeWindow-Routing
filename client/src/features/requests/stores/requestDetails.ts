import { defineStore } from "pinia";
import { ref } from "vue";
import { changeRequestStatus, fetchRequest, toApiFailure, type ApiFailure, type RequestStatusAction } from "../api/requestsApi";
import type { RequestDetailed } from "../types";

export type RequestDetailsLoadState = { kind: "loading" } | { kind: "ready"; data: RequestDetailed } | ApiFailure;

const ACTION_FAILURE: Record<RequestStatusAction, string> = {
  in_progress: "Не удалось начать выполнение заявки",
  complete: "Не удалось завершить заявку",
  cancel: "Не удалось отменить заявку",
};

export const useRequestDetailsStore = defineStore("requests/details", () => {
  const state = ref<RequestDetailsLoadState>({ kind: "loading" });
  /** the status change in flight for the shown request */
  const pendingAction = ref<RequestStatusAction | null>(null);
  /** why the last status change failed; cleared when another request is shown or on the next try */
  const actionError = ref<string | null>(null);
  // Only the latest request may update the state (quick back/forward between requests)
  let requestSeq = 0;

  async function load(id: number): Promise<void> {
    const seq = ++requestSeq;
    // a different request never shows the previous one's data (or its action error) meanwhile
    if (state.value.kind !== "ready" || state.value.data.id !== id) {
      state.value = { kind: "loading" };
      actionError.value = null;
    }

    let next: RequestDetailsLoadState;
    try {
      const data = await fetchRequest(id);
      next = data ? { kind: "ready", data } : { kind: "not-found", message: null, status: null };
    } catch (e) {
      next = toApiFailure(e);
    }
    if (seq === requestSeq) state.value = next;
  }

  /** Starts work on, completes or cancels the request, then reloads it so the page shows its new status */
  async function changeStatus(id: number, action: RequestStatusAction): Promise<void> {
    if (pendingAction.value) return;
    pendingAction.value = action;
    actionError.value = null;
    try {
      await changeRequestStatus(id, action);
    } catch (e) {
      const failure = toApiFailure(e);
      actionError.value =
        failure.message ?? `${ACTION_FAILURE[action]}${failure.status ? ` (HTTP ${failure.status})` : ""}`;
    } finally {
      pendingAction.value = null;
    }
    // also after a failure: a 409 usually means the page shows an out-of-date status
    await load(id);
  }

  return { state, pendingAction, actionError, load, changeStatus };
});
