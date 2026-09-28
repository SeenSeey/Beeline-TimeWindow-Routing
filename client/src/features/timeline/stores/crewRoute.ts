import { defineStore } from "pinia";
import { ref, shallowRef } from "vue";
import { fetchCrewRoute, toApiFailure } from "../api/timelineApi";
import type { CrewRoute } from "../types";

/**
 * The road route of the crew selected on the board, loaded on demand — the schedule itself only carries
 * each crew's route length. Routes are kept per schedule: switching back to a crew doesn't refetch it.
 */
export const useCrewRouteStore = defineStore("timeline/crewRoute", () => {
  /** the shown crew's route; null while loading, when there is none or it failed to load */
  const route = shallowRef<CrewRoute | null>(null);
  const loading = ref(false);

  /** the schedule the cached routes belong to */
  let cachedScheduleId: number | null = null;
  /** crew id → its loaded route */
  const cache = new Map<number, CrewRoute>();
  // Only the latest request may update the state (quick switching between crews)
  let requestSeq = 0;

  async function load(scheduleId: number, crewId: number): Promise<void> {
    const seq = ++requestSeq;
    if (cachedScheduleId !== scheduleId) {
      cachedScheduleId = scheduleId;
      cache.clear();
    }

    const cached = cache.get(crewId);
    if (cached) {
      route.value = cached;
      loading.value = false;
      return;
    }

    route.value = null;
    loading.value = true;
    let loaded: CrewRoute | null;
    try {
      loaded = await fetchCrewRoute(scheduleId, crewId);
    } catch (e) {
      // 404: no route stored for the crew — the map shows the pins only; anything else is logged by toApiFailure
      toApiFailure(e);
      loaded = null;
    }

    if (seq !== requestSeq) return;
    // a failure isn't cached: the next selection tries again
    if (loaded) cache.set(crewId, loaded);
    route.value = loaded;
    loading.value = false;
  }

  /** No crew to show (none selected, or the schedule has no routes yet) */
  function clear(): void {
    requestSeq++;
    route.value = null;
    loading.value = false;
  }

  return { route, loading, load, clear };
});
