import type { JobTypeKeyword, RequestStatusKeyword } from "../types";

// The server has no dictionary endpoints yet, and the filters take numeric ids.
// The ids mirror the insertion order of DemoDatasetLoader (enum declaration order);
// replace these with API calls once dictionary endpoints exist.

export interface DictionaryEntry<K extends string> {
  id: number;
  keyword: K;
  label: string;
}

export const REQUEST_STATUSES: DictionaryEntry<RequestStatusKeyword>[] = [
  { id: 1, keyword: "IDLE", label: "Не назначена" },
  { id: 2, keyword: "ASSIGNED", label: "Назначена" },
  { id: 3, keyword: "IN_PROGRESS", label: "В работе" },
  { id: 4, keyword: "COMPLETED", label: "Выполнена" },
  { id: 5, keyword: "CANCELED", label: "Отменена" },
];

export const JOB_TYPES: DictionaryEntry<JobTypeKeyword>[] = [
  { id: 1, keyword: "LOCAL_WORK", label: "Локальная заявка" },
  { id: 2, keyword: "INSTALLATION", label: "Подключение" },
  { id: 3, keyword: "ADDON", label: "Дозаказ оборудования" },
  { id: 4, keyword: "EMERGENCY", label: "Глобальная проблема" },
];

/** Display name of a status keyword; unknown keywords are shown as-is */
export function statusLabel(keyword: string): string {
  return REQUEST_STATUSES.find((s) => s.keyword === keyword)?.label ?? keyword;
}
