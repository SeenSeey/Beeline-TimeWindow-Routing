import { defineStore } from "pinia";
import { ref } from "vue";
import { changeRequestStatus, type RequestStatusAction } from "@/features/requests/api/requestsApi";
import { toApiFailure } from "../api/timelineApi";
import { useScheduleStore } from "./schedule";

/** The work bar whose status menu is open, and where on screen */
export interface VisitStatusMenuTarget {
  requestId: number;
  x: number;
  y: number;
}

const ACTION_FAILURE: Record<RequestStatusAction, string> = {
  in_progress: "Не удалось начать выполнение заявки",
  complete: "Не удалось завершить заявку",
  cancel: "Не удалось отменить заявку",
};

/**
 * The timeline's right-click menu on a work bar: start work on, complete or cancel the request. After a
 * change the region's schedule is reloaded, so the bar shows its new status.
 */
export const useVisitStatusStore = defineStore("timeline/visitStatus", () => {
  const scheduleStore = useScheduleStore();

  const menu = ref<VisitStatusMenuTarget | null>(null);
  /** the status change in flight */
  const pendingAction = ref<RequestStatusAction | null>(null);
  /** why the last change failed; shown in the menu, cleared when it's opened again */
  const error = ref<string | null>(null);

  function openMenu(target: VisitStatusMenuTarget): void {
    if (pendingAction.value) return;
    menu.value = target;
    error.value = null;
  }

  function closeMenu(): void {
    if (pendingAction.value) return;
    menu.value = null;
    error.value = null;
  }

  async function changeStatus(requestId: number, action: RequestStatusAction): Promise<void> {
    const regionId = scheduleStore.schedule?.region.id;
    if (pendingAction.value || regionId == null) return;
    pendingAction.value = action;
    error.value = null;
    try {
      await changeRequestStatus(requestId, action);
      pendingAction.value = null;
      menu.value = null;
    } catch (e) {
      const failure = toApiFailure(e);
      error.value = failure.message ?? `${ACTION_FAILURE[action]}${failure.status ? ` (HTTP ${failure.status})` : ""}`;
      pendingAction.value = null;
    }
    // also after a failure: a 409 usually means the timeline shows an out-of-date status
    await scheduleStore.load(regionId);
  }

  return { menu, pendingAction, error, openMenu, closeMenu, changeStatus };
});
