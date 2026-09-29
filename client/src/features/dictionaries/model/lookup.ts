import type { DictionaryItem } from "../types";

// Lookups in a dictionary loaded from the server: the lists carry the real ids, keywords and names

/** the entry's display name by its keyword; the keyword itself while the list hasn't loaded (or lacks it) */
export const displayNameOf = (items: DictionaryItem[], keyword: string): string =>
  items.find((item) => item.keyword === keyword)?.displayName ?? keyword;

/** the entry's keyword by its id; null when unknown */
export const keywordOf = (items: DictionaryItem[], id: number | null): string | null =>
  id == null ? null : (items.find((item) => item.id === id)?.keyword ?? null);

/** select options { value: id, label: name } */
export const toOptions = (items: DictionaryItem[]): { value: number; label: string }[] =>
  items.map((item) => ({ value: item.id, label: item.displayName }));
