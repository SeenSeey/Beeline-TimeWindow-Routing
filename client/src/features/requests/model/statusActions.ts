import type { RequestStatusAction } from "../api/requestsApi";

// Which status changes a request allows — the same rules the server applies
// (Request.markInProgress / complete / cancel). Used by the request page and the timeline's menu.

export interface RequestProgress {
  /** status keyword, e.g. "IN_PROGRESS"; null when the server sent none */
  statusKeyword: string | null;
  /** ISO LocalDateTime or null */
  completedAt: string | null;
  canceledAt: string | null;
}

export function availableStatusActions(request: RequestProgress): Record<RequestStatusAction, boolean> {
  const canceled = request.canceledAt != null || request.statusKeyword === "CANCELED";
  const completed = request.completedAt != null || request.statusKeyword === "COMPLETED";
  const inProgress = request.statusKeyword === "IN_PROGRESS";
  return {
    // work starts once
    in_progress: !canceled && !completed && !inProgress,
    // only a request in work
    complete: inProgress,
    // only a request that hasn't started yet
    cancel: !canceled && !completed && !inProgress,
  };
}

/** Button texts: the action, and what it shows while it runs */
export const STATUS_ACTION_LABELS: Record<RequestStatusAction, { idle: string; running: string }> = {
  in_progress: { idle: "Взять в работу", running: "Начинаем…" },
  complete: { idle: "Завершить", running: "Завершаем…" },
  cancel: { idle: "Отменить заявку", running: "Отменяем…" },
};
