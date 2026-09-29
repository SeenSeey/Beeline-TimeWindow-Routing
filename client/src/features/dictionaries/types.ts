// Server DTOs of the dictionaries feature

/** DictionaryItemDto — an entry of a dictionary, as selectors and filters offer it */
export interface DictionaryItem {
  /** the database id: what filters and forms send back */
  id: number;
  /** the server enum's name (logic, styling); null for equipment, which has none */
  keyword: string | null;
  displayName: string;
}
