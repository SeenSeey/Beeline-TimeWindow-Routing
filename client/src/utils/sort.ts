// A list's sort order, shared by the paged tables (requests, crews): kept in the URL (?sort=createdAt&dir=desc)
// and sent to the server as `sort=createdAt,desc`

export type SortDirection = "asc" | "desc";

export interface SortOrder<F extends string> {
  field: F;
  direction: SortDirection;
}

/** the column clicked: ascending, then descending, then unsorted; another column starts ascending */
export function nextSortOrder<F extends string>(current: SortOrder<F> | null, field: F): SortOrder<F> | null {
  if (current?.field !== field) return { field, direction: "asc" };
  return current.direction === "asc" ? { field, direction: "desc" } : null;
}

/** ?sort=&dir= → the order, if the field is one the list can sort by */
export function readSortOrder<F extends string>(params: URLSearchParams, fields: readonly F[]): SortOrder<F> | null {
  const field = params.get("sort");
  if (!field || !(fields as readonly string[]).includes(field)) return null;
  return { field: field as F, direction: params.get("dir") === "desc" ? "desc" : "asc" };
}

/** the URL entries of the order (none when unsorted) */
export function sortUrlEntries<F extends string>(order: SortOrder<F> | null): [string, string | null][] {
  return [
    ["sort", order?.field ?? null],
    ["dir", order?.field ? order.direction : null],
  ];
}

/** the `sort` query param of the list endpoints */
export const sortApiParam = <F extends string>(order: SortOrder<F>): string => `${order.field},${order.direction}`;
