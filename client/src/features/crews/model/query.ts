import { readSortOrder, sortApiParam, sortUrlEntries, type SortOrder } from "@/utils/sort";
import type { CrewFilters } from "../types";

// Filters, sort and the page live in the URL (?name=север&sort=name&dir=desc&page=2), so reloads and shared
// links keep them

export const PAGE_SIZES = [10, 20, 50] as const;
export const DEFAULT_PAGE_SIZE = 20;

/** the columns the server can sort crews by (CrewController.SORTABLE_FIELDS) */
export const CREW_SORT_FIELDS = ["id", "name", "region", "vehicle"] as const;
export type CrewSortField = (typeof CREW_SORT_FIELDS)[number];

export interface CrewsQuery {
  filters: CrewFilters;
  /** null: the server's default, by number */
  sort: SortOrder<CrewSortField> | null;
  /** 1-based */
  page: number;
  limit: number;
}

export const EMPTY_FILTERS: CrewFilters = {
  name: null,
  regionId: null,
  vehicleId: null,
  skillId: null,
};

function readId(value: string | null): number | null {
  const id = Number(value);
  return value != null && Number.isInteger(id) && id > 0 ? id : null;
}

function readText(value: string | null): string | null {
  const text = value?.trim();
  return text ? text : null;
}

export function readQuery(params: URLSearchParams): CrewsQuery {
  const limit = Number(params.get("limit"));
  return {
    filters: {
      name: readText(params.get("name")),
      regionId: readId(params.get("region")),
      vehicleId: readId(params.get("vehicle")),
      skillId: readId(params.get("skill")),
    },
    sort: readSortOrder(params, CREW_SORT_FIELDS),
    page: readId(params.get("page")) ?? 1,
    limit: (PAGE_SIZES as readonly number[]).includes(limit) ? limit : DEFAULT_PAGE_SIZE,
  };
}

/** URL query for the router; defaults are left out to keep links short */
export function toUrlQuery({ filters, sort, page, limit }: CrewsQuery): Record<string, string> {
  const entries: [string, unknown][] = [
    ["name", filters.name],
    ["region", filters.regionId],
    ["vehicle", filters.vehicleId],
    ["skill", filters.skillId],
    ...sortUrlEntries(sort),
    ["page", page === 1 ? null : page],
    ["limit", limit === DEFAULT_PAGE_SIZE ? null : limit],
  ];
  return Object.fromEntries(entries.filter(([, value]) => value != null).map(([key, value]) => [key, String(value)]));
}

/** Query params of GET /api/v1/crews; unset filters are omitted */
export function toApiParams({ filters, sort, page, limit }: CrewsQuery): Record<string, string | number> {
  const params: Record<string, string | number> = { page, limit };
  for (const [key, value] of Object.entries(filters)) if (value != null) params[key] = value;
  if (sort) params.sort = sortApiParam(sort);
  return params;
}

export function hasActiveFilters(filters: CrewFilters): boolean {
  return Object.values(filters).some((value) => value != null);
}
