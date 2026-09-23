import { defineStore } from "pinia";
import { computed, ref, watch } from "vue";
import { collectJobTypes } from "../model/colors";
import { buildAxis, buildCrewRows, visitKey } from "../model/timeline";
import type { JobTypeKeyword } from "../types";
import { useScheduleStore } from "./schedule";

export const ZOOM_MIN = 1;
export const ZOOM_MAX = 6;
const ZOOM_LEVELS = [1, 1.5, 2, 3, 4, 6];

const clampZoom = (z: number): number => Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, Math.round(z * 100) / 100));

// Filters and zoom persist between visits
const STORAGE_KEY = "board-state";
interface SavedPrefs {
  jobType?: unknown;
  onlyUrgent?: unknown;
  zoom?: unknown;
}
function readPrefs(): SavedPrefs {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}") as SavedPrefs;
  } catch {
    return {};
  }
}

/** View state of the board and everything derived from the loaded schedule */
export const useBoardStore = defineStore("timeline/board", () => {
  const scheduleStore = useScheduleStore();
  const schedule = computed(() => scheduleStore.schedule);

  // ── Preferences ──
  const saved = readPrefs();
  /** job type keyword to filter by; "" shows all (checked against the schedule's job types below) */
  const jobType = ref<JobTypeKeyword | "">(typeof saved.jobType === "string" ? (saved.jobType as JobTypeKeyword) : "");
  const onlyUrgent = ref(typeof saved.onlyUrgent === "boolean" ? saved.onlyUrgent : false);
  const zoom = ref(typeof saved.zoom === "number" ? clampZoom(saved.zoom) : 1);

  watch([jobType, onlyUrgent, zoom], () => {
    try {
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({ jobType: jobType.value, onlyUrgent: onlyUrgent.value, zoom: zoom.value }),
      );
    } catch {
      /* storage unavailable */
    }
  });

  // ── Derived schedule model ──
  const axis = computed(() => buildAxis(schedule.value?.engineers ?? []));

  const jobTypes = computed(() =>
    collectJobTypes(
      (schedule.value?.engineers ?? []).flatMap((e) =>
        e.slots.filter((s) => s.slotType === "REQUEST").map((s) => s.jobType),
      ),
    ),
  );

  const crews = computed(() => (schedule.value ? buildCrewRows(schedule.value, axis.value) : []));
  const visits = computed(() => crews.value.flatMap((c) => c.requests));
  const visitsByKey = computed(() => new Map(visits.value.map((v) => [visitKey(v), v])));
  const unassigned = computed(() => schedule.value?.unassignedRequests ?? []);

  const visibleCrews = computed(() =>
    crews.value.filter(
      (c) =>
        (!jobType.value || c.requests.some((v) => v.slot.jobType.keyword === jobType.value)) &&
        (!onlyUrgent.value || c.requests.some((v) => v.urgent)),
    ),
  );

  // ── Crew selection ──
  const selectedCrewId = ref<number | null>(null);
  const selectedCrew = computed(() => crews.value.find((c) => c.id === selectedCrewId.value) ?? null);

  function toggleCrew(id: number): void {
    selectedCrewId.value = selectedCrewId.value === id ? null : id;
  }

  // A new schedule (e.g. another region): preselect the first busy crew and drop a job type filter
  // this schedule doesn't have — it would hide every crew
  watch(
    schedule,
    () => {
      selectedCrewId.value = crews.value.find((c) => c.requests.length)?.id ?? null;
      if (jobType.value && !jobTypes.value.some((j) => j.keyword === jobType.value)) jobType.value = "";
    },
    { immediate: true },
  );

  // Drop the selection when filters hide the selected crew
  watch(visibleCrews, (list) => {
    if (selectedCrewId.value != null && !list.some((c) => c.id === selectedCrewId.value)) selectedCrewId.value = null;
  });

  // ── Zoom ──
  function setZoom(z: number): void {
    zoom.value = clampZoom(z);
  }
  function zoomIn(): void {
    setZoom(ZOOM_LEVELS.find((z) => z > zoom.value) ?? ZOOM_MAX);
  }
  function zoomOut(): void {
    setZoom([...ZOOM_LEVELS].reverse().find((z) => z < zoom.value) ?? ZOOM_MIN);
  }

  return {
    jobType,
    onlyUrgent,
    zoom,
    axis,
    jobTypes,
    crews,
    visits,
    visitsByKey,
    unassigned,
    visibleCrews,
    selectedCrewId,
    selectedCrew,
    toggleCrew,
    setZoom,
    zoomIn,
    zoomOut,
  };
});
