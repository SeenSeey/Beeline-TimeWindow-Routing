import type { SkillKeyword, VehicleKeyword } from "../types";

// The server has no dictionary endpoints yet, and the filters take numeric ids.
// The ids mirror the insertion order of DemoDatasetLoader (enum declaration order), like the
// request statuses and job types in features/requests/model/dictionaries.ts; replace these with
// API calls once dictionary endpoints exist.

export interface DictionaryEntry<K extends string> {
  id: number;
  keyword: K;
  label: string;
}

export const VEHICLES: DictionaryEntry<VehicleKeyword>[] = [
  { id: 1, keyword: "PEDESTRIAN", label: "Пешком" },
  { id: 2, keyword: "BICYCLE", label: "Велосипед" },
  { id: 3, keyword: "PUBLIC_TRANSPORT", label: "Общественный транспорт" },
  { id: 4, keyword: "CAR", label: "Автомобиль" },
];

export const SKILLS: DictionaryEntry<SkillKeyword>[] = [
  { id: 1, keyword: "LOCAL_WORK", label: "Локальные работы" },
  { id: 2, keyword: "INSTALLATION_OR_ADDON", label: "Подключения и дозаказы" },
  { id: 3, keyword: "EMERGENCY", label: "Аварийные работы" },
];

/** Display name of a keyword; unknown keywords are shown as-is */
const labelOf = (entries: DictionaryEntry<string>[], keyword: string): string =>
  entries.find((entry) => entry.keyword === keyword)?.label ?? keyword;

export const vehicleLabel = (keyword: string): string => labelOf(VEHICLES, keyword);
export const skillLabel = (keyword: string): string => labelOf(SKILLS, keyword);
