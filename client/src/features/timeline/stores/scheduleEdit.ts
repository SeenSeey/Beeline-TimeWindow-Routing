import { defineStore } from "pinia";
import { computed, ref, shallowRef, watch } from "vue";
import { previewScheduleEdit, saveScheduleEdit, toApiFailure } from "../api/timelineApi";
import {
  applyPreview,
  changedRoutes,
  draftFromCrews,
  withoutRequest,
  withRequestAt,
  type RouteDraft,
} from "../model/scheduleDraft";
import type { CrewRow } from "../model/timeline";
import type { CrewRouteDraft, RouteViolation, SchedulePreview } from "../types";
import { useScheduleStore } from "./schedule";

export interface EditNotice {
  kind: "error" | "success";
  message: string;
}

/** The request whose action menu is open, and where on screen */
export interface EditMenuTarget {
  requestId: number;
  x: number;
  y: number;
}

/**
 * The draft requests the preview doesn't account for: every stop sent must come back on its crew's row
 * or, if it can't be timed, as a violation
 */
function missingFromPreview(routes: CrewRouteDraft[], preview: SchedulePreview): number[] {
  const returned = new Set<number>();
  for (const row of preview.engineers) for (const slot of row.slots) returned.add(slot.requestId);
  for (const violation of preview.violations) returned.add(violation.requestId);
  return routes.flatMap((route) => route.stops.map((stop) => stop.requestId)).filter((id) => !returned.has(id));
}

/**
 * Edit mode of the schedule board: a local draft of the crews' routes. A change is placed exactly where
 * the dispatcher put it, then checked by the server: everything wrong with a visit (the crew lacks the
 * required vehicle, the work starts outside the request's time window, it overlaps another visit of the
 * crew) is shown on it, and saving stays blocked until the draft is clean. Nothing is written until save().
 */
export const useScheduleEditStore = defineStore("timeline/scheduleEdit", () => {
  const scheduleStore = useScheduleStore();

  const active = ref(false);
  const regionId = ref<number | null>(null);
  /** the routes as saved when editing started */
  const saved = shallowRef<RouteDraft>(new Map());
  /** the routes with every change made so far */
  const draft = shallowRef<RouteDraft>(new Map());
  /** the server's check of the draft; null while the draft equals the saved routes */
  const preview = shallowRef<SchedulePreview | null>(null);
  const busy = ref<"previewing" | "saving" | null>(null);
  /** a failed request (the check or the save) or a finished save; rule violations are in `violations` */
  const notice = ref<EditNotice | null>(null);
  const menu = ref<EditMenuTarget | null>(null);
  /** crew ids in the order the board showed them when editing started; kept while editing */
  const crewOrder = shallowRef<number[]>([]);
  // Only the latest check may update the state
  let previewSeq = 0;

  const changes = computed(() => changedRoutes(saved.value, draft.value));
  const changedCrewIds = computed(() => new Set(changes.value.map((route) => route.crewId)));

  /** what is wrong with the draft, by the server's last check */
  const violations = computed<RouteViolation[]>(() => (changes.value.length ? (preview.value?.violations ?? []) : []));

  /** request id → the rules its visit breaks */
  const violationsByRequest = computed(() => {
    const byRequest = new Map<number, string[]>();
    for (const violation of violations.value) {
      byRequest.set(violation.requestId, [...(byRequest.get(violation.requestId) ?? []), violation.message]);
    }
    return byRequest;
  });

  /** only a checked, clean draft with changes can be saved */
  const canSave = computed(
    () =>
      active.value && !busy.value && changes.value.length > 0 && preview.value !== null && violations.value.length === 0,
  );

  /** what the board shows while editing: the saved schedule with the draft's checked rows */
  const previewSchedule = computed(() => {
    const schedule = scheduleStore.schedule;
    if (!active.value || !schedule || !preview.value || !changes.value.length) return null;
    return applyPreview(schedule, preview.value);
  });

  function start(forRegionId: number, crews: CrewRow[]): void {
    const initial = draftFromCrews(crews);
    active.value = true;
    regionId.value = forRegionId;
    crewOrder.value = crews.map((crew) => crew.id);
    saved.value = initial;
    draft.value = initial;
    preview.value = null;
    notice.value = null;
    menu.value = null;
  }

  function stop(): void {
    previewSeq++;
    active.value = false;
    regionId.value = null;
    crewOrder.value = [];
    saved.value = new Map();
    draft.value = new Map();
    preview.value = null;
    busy.value = null;
    menu.value = null;
  }

  /**
   * Makes the change and has the server check the result. The change stays even when it breaks a rule —
   * the violations are shown and block saving. Only when the check itself fails (network, server error)
   * is the change taken back, since it could not be checked.
   */
  async function propose(next: RouteDraft): Promise<void> {
    if (!active.value || regionId.value == null || busy.value) return;
    menu.value = null;
    notice.value = null;

    const previous = { draft: draft.value, preview: preview.value };
    draft.value = next;

    const routes = changedRoutes(saved.value, next);
    if (!routes.length) {
      // back to the saved routes: nothing to check
      preview.value = null;
      return;
    }

    const seq = ++previewSeq;
    busy.value = "previewing";
    try {
      const result = await previewScheduleEdit(regionId.value, routes);
      if (seq !== previewSeq) return;
      const lost = missingFromPreview(routes, result);
      if (lost.length) {
        // never show a route the server dropped visits from (e.g. client and server out of date)
        draft.value = previous.draft;
        preview.value = previous.preview;
        notice.value = {
          kind: "error",
          message: `Изменение отменено: сервер вернул маршрут без заявок ${lost.map((id) => `#${id}`).join(", ")}. Обновите страницу; если повторится — проверьте, что сервер обновлён.`,
        };
        return;
      }
      preview.value = result;
    } catch (e) {
      if (seq !== previewSeq) return;
      draft.value = previous.draft;
      preview.value = previous.preview;
      const failure = toApiFailure(e);
      notice.value = {
        kind: "error",
        message: `Не удалось проверить изменение, оно отменено${failure.message ? `: ${failure.message}` : ""}`,
      };
    } finally {
      if (seq === previewSeq) busy.value = null;
    }
  }

  /** Takes the request out of the schedule (it goes to the unassigned list) */
  const remove = (requestId: number): Promise<void> => propose(withoutRequest(draft.value, requestId));

  /**
   * Puts the request on the crew's route at `index`, starting at `startTime` ("HH:mm:ss", where it was
   * dropped) or, when null, as early as possible: adds, moves or reorders it
   */
  const place = (requestId: number, crewId: number, index: number, startTime: string | null): Promise<void> =>
    propose(withRequestAt(draft.value, requestId, crewId, index, startTime));

  /** Throws all changes away, staying in edit mode */
  function discardChanges(): void {
    previewSeq++;
    draft.value = saved.value;
    preview.value = null;
    busy.value = null;
    notice.value = null;
    menu.value = null;
  }

  /** Saves the draft; on success the board shows the saved schedule and edit mode ends */
  async function save(): Promise<void> {
    if (!canSave.value || regionId.value == null) return;
    busy.value = "saving";
    notice.value = null;
    try {
      const updated = await saveScheduleEdit(regionId.value, changes.value);
      if (updated) scheduleStore.replace(updated);
      stop();
      notice.value = { kind: "success", message: "Изменения расписания сохранены." };
    } catch (e) {
      const failure = toApiFailure(e);
      notice.value = { kind: "error", message: failure.message ?? "Не удалось сохранить изменения" };
      busy.value = null;
    }
  }

  function openMenu(target: EditMenuTarget): void {
    if (active.value) menu.value = target;
  }

  const closeMenu = (): void => {
    menu.value = null;
  };

  const dismissNotice = (): void => {
    notice.value = null;
  };

  // Another region (or a reload into a different schedule) ends editing: the draft belongs to the old one
  watch(
    () => scheduleStore.schedule?.region.id,
    (shownRegionId) => {
      if (active.value && shownRegionId !== regionId.value) stop();
    },
  );

  return {
    active,
    draft,
    crewOrder,
    busy,
    notice,
    menu,
    changes,
    changedCrewIds,
    violations,
    violationsByRequest,
    canSave,
    previewSchedule,
    start,
    stop,
    remove,
    place,
    discardChanges,
    save,
    openMenu,
    closeMenu,
    dismissNotice,
  };
});
