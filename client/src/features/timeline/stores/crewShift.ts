import { defineStore } from "pinia";
import { ref } from "vue";
import { takeCrewOffShift } from "@/features/crews/api/crewsApi";
import { toApiFailure } from "../api/timelineApi";
import { useScheduleStore } from "./schedule";

/** The crew whose menu is open, and where on screen */
export interface CrewShiftMenuTarget {
  crewId: number;
  x: number;
  y: number;
}

/**
 * The timeline's right-click menu on a crew row: take the crew off today's shift. The server then
 * recalculates the schedule without it, so the region's schedule is reloaded (and polled while CALCULATING).
 */
export const useCrewShiftStore = defineStore("timeline/crewShift", () => {
  const scheduleStore = useScheduleStore();

  const menu = ref<CrewShiftMenuTarget | null>(null);
  /** taking the crew off shift is in flight */
  const pending = ref(false);
  /** why the last attempt failed; shown in the menu, cleared when it's opened again */
  const error = ref<string | null>(null);

  function openMenu(target: CrewShiftMenuTarget): void {
    if (pending.value) return;
    menu.value = target;
    error.value = null;
  }

  function closeMenu(): void {
    if (pending.value) return;
    menu.value = null;
    error.value = null;
  }

  async function takeOffShift(crewId: number): Promise<void> {
    const regionId = scheduleStore.schedule?.region.id;
    if (pending.value || regionId == null) return;
    pending.value = true;
    error.value = null;
    try {
      await takeCrewOffShift(crewId);
      pending.value = false;
      menu.value = null;
    } catch (e) {
      const failure = toApiFailure(e);
      error.value =
        failure.message ?? `Не удалось снять бригаду со смены${failure.status ? ` (HTTP ${failure.status})` : ""}`;
      pending.value = false;
    }
    // also after a failure: a 409 usually means the crew is already off shift and the board is out of date
    await scheduleStore.load(regionId);
  }

  return { menu, pending, error, openMenu, closeMenu, takeOffShift };
});
