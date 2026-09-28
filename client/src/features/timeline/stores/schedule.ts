import { defineStore } from "pinia";
import { computed, ref, shallowRef } from "vue";
import { fetchSchedule, fetchScheduleVersion, toApiFailure, type ApiFailure } from "../api/timelineApi";
import { isScheduleCalculating } from "../model/scheduleStatus";
import type { ScheduleData } from "../types";

export type ScheduleLoadState = { kind: "loading" } | { kind: "ready"; data: ScheduleData } | ApiFailure;

/** how often a schedule that is still being calculated is refetched */
const CALCULATION_POLL_MS = 1000;

export const useScheduleStore = defineStore("timeline/schedule", () => {
  const state = ref<ScheduleLoadState>({ kind: "loading" });
  /** a region switch is in flight; the current view stays visible meanwhile */
  const switching = ref(false);
  // Only the latest request may update the state (fast region switching)
  let requestSeq = 0;
  // Pending refetch of a schedule that is still CALCULATING
  let pollTimer: ReturnType<typeof setTimeout> | null = null;

  /** the region's current version, as loaded (and polled while it is calculated) */
  const current = computed(() => (state.value.kind === "ready" ? state.value.data : null));

  // ── Earlier versions ──
  // A recalculation replaces the schedule with a new version that points back at the one before
  // (previousScheduleId). An earlier version can be looked at in the current one's place, read-only.

  /** the earlier version being looked at; null: the current one is shown */
  const viewedVersion = shallowRef<ScheduleData | null>(null);
  /** ids of the versions stepped back through, oldest last: "newer" retraces it (versions only link backwards) */
  const versionTrail = ref<number[]>([]);
  const versionLoading = ref(false);
  /** why the last version failed to load */
  const versionError = ref<string | null>(null);
  /** earlier versions never change: each is loaded once */
  const versionCache = new Map<number, ScheduleData>();
  // Only the latest version request may update the state
  let versionSeq = 0;

  /** what the board shows: the earlier version being looked at, or the current one */
  const schedule = computed(() => viewedVersion.value ?? current.value);
  /** an earlier version is shown: nothing on the board may be changed */
  const viewingHistory = computed(() => viewedVersion.value !== null);

  /** Steps back to the version the shown one replaced */
  async function showOlderVersion(): Promise<void> {
    const olderId = schedule.value?.previousScheduleId;
    if (olderId == null || versionLoading.value) return;

    const seq = ++versionSeq;
    versionError.value = null;
    let older = versionCache.get(olderId) ?? null;
    if (!older) {
      versionLoading.value = true;
      try {
        older = await fetchScheduleVersion(olderId);
      } catch (e) {
        const failure = toApiFailure(e);
        if (seq === versionSeq) versionError.value = failure.message ?? "Не удалось загрузить предыдущую версию";
      }
      if (seq !== versionSeq) return;
      versionLoading.value = false;
      if (!older) return;
      versionCache.set(olderId, older);
    }

    versionTrail.value = [...versionTrail.value, olderId];
    viewedVersion.value = older;
  }

  /** Steps forward to the version that replaced the shown one — the current one after the last step */
  function showNewerVersion(): void {
    versionSeq++;
    versionLoading.value = false;
    versionError.value = null;
    const trail = versionTrail.value.slice(0, -1);
    versionTrail.value = trail;
    const newerId = trail[trail.length - 1];
    viewedVersion.value = newerId == null ? null : (versionCache.get(newerId) ?? null);
  }

  /** Back to the current version */
  function showCurrentVersion(): void {
    versionSeq++;
    versionLoading.value = false;
    versionError.value = null;
    versionTrail.value = [];
    viewedVersion.value = null;
  }

  function stopPolling(): void {
    if (pollTimer != null) clearTimeout(pollTimer);
    pollTimer = null;
  }

  /** Back to the initial loading screen, dropping any request in flight */
  function reset(): void {
    requestSeq++;
    stopPolling();
    showCurrentVersion();
    versionCache.clear();
    switching.value = false;
    state.value = { kind: "loading" };
  }

  function failWith(failure: ApiFailure): void {
    requestSeq++;
    stopPolling();
    showCurrentVersion();
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
    // another region: its own versions, starting from the current one
    if (current.value && current.value.region.id !== regionId) showCurrentVersion();
    if (!background) switching.value = state.value.kind !== "loading";

    let next: ScheduleLoadState;
    try {
      const data = await fetchSchedule(regionId);
      next = data ? { kind: "ready", data } : { kind: "not-found", message: null, status: null };
    } catch (e) {
      next = toApiFailure(e);
    }

    if (seq !== requestSeq) return;

    const stillCalculating = next.kind === "ready" && isScheduleCalculating(next.data.status.keyword);
    const keepCurrentView = background && next.kind !== "ready";
    if (!keepCurrentView) state.value = next;
    switching.value = false;

    if (stillCalculating || keepCurrentView) schedulePoll(regionId);
  }

  function schedulePoll(regionId: number): void {
    pollTimer = setTimeout(() => void load(regionId, { background: true }), CALCULATION_POLL_MS);
  }

  /**
   * Counts the times the shown schedule changed in place (replace(): a saved edit keeps its id) — what the
   * crews' routes were loaded for is then out of date
   */
  const revision = ref(0);

  /** Shows a schedule the server returned from another call (e.g. after saving an edit) */
  function replace(data: ScheduleData): void {
    revision.value++;
    requestSeq++;
    stopPolling();
    switching.value = false;
    state.value = { kind: "ready", data };
    // the server may still be recalculating it: refetch until it is ready, as load() does
    if (isScheduleCalculating(data.status.keyword)) schedulePoll(data.region.id);
  }

  return {
    state,
    switching,
    revision,
    current,
    schedule,
    viewingHistory,
    versionTrail,
    versionLoading,
    versionError,
    reset,
    fail,
    failWith,
    load,
    replace,
    stopPolling,
    showOlderVersion,
    showNewerVersion,
    showCurrentVersion,
  };
});
