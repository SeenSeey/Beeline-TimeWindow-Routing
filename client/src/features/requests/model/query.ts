import { readSortOrder, sortApiParam, sortUrlEntries, type SortOrder } from "@/utils/sort";
import type { RequestFilters } from "../types";

// Filters, sort and the page live in the URL (?status=2&sort=createdAt&dir=desc&page=3), so reloads and
// shared links keep them

export const PAGE_SIZES = [10, 20, 50] as const;
export const DEFAULT_PAGE_SIZE = 20;

/** the columns the server can sort requests by (RequestController.SORTABLE_FIELDS) */
export const REQUEST_SORT_FIELDS = ["id", "createdAt", "address", "status", "jobType", "region", "crew", "urgent"] as const;
export type RequestSortField = (typeof REQUEST_SORT_FIELDS)[number];

export interface RequestsQuery {
  filters: RequestFilters;
  /** null: the server's default, by number */
  sort: SortOrder<RequestSortField> | null;
  /** 1-based */
  page: number;
  limit: number;
}

export const EMPTY_FILTERS: RequestFilters = {
  statusId: null,
  urgent: null,
  regionId: null,
  jobTypeId: null,
  address: null,
  crewName: null,
};

function readId(value: string | null): number | null {
  const id = Number(value);
  return value != null && Number.isInteger(id) && id > 0 ? id : null;
}

function readBool(value: string | null): boolean | null {
  return value === "true" ? true : value === "false" ? false : null;
}

function readText(value: string | null): string | null {
  const text = value?.trim();
  return text ? text : null;
}

export function readQuery(params: URLSearchParams): RequestsQuery {
  const limit = Number(params.get("limit"));
  return {
    filters: {
      statusId: readId(params.get("status")),
      urgent: readBool(params.get("urgent")),
      regionId: readId(params.get("region")),
      jobTypeId: readId(params.get("jobType")),
      address: readText(params.get("address")),
      crewName: readText(params.get("crew")),
    },
    sort: readSortOrder(params, REQUEST_SORT_FIELDS),
    page: readId(params.get("page")) ?? 1,
    limit: (PAGE_SIZES as readonly number[]).includes(limit) ? limit : DEFAULT_PAGE_SIZE,
  };
}

/** URL query for the router; defaults are left out to keep links short */
export function toUrlQuery({ filters, sort, page, limit }: RequestsQuery): Record<string, string> {
  const entries: [string, unknown][] = [
    ["status", filters.statusId],
    ["urgent", filters.urgent],
    ["region", filters.regionId],
    ["jobType", filters.jobTypeId],
    ["address", filters.address],
    ["crew", filters.crewName],
    ...sortUrlEntries(sort),
    ["page", page === 1 ? null : page],
    ["limit", limit === DEFAULT_PAGE_SIZE ? null : limit],
  ];
  return Object.fromEntries(entries.filter(([, v]) => v != null).map(([k, v]) => [k, String(v)]));
}

/** Query params of GET /api/v1/requests; unset filters are omitted */
export function toApiParams({ filters, sort, page, limit }: RequestsQuery): Record<string, string | number | boolean> {
  const params: Record<string, string | number | boolean> = { page, limit };
  for (const [key, value] of Object.entries(filters)) if (value != null) params[key] = value;
  if (sort) params.sort = sortApiParam(sort);
  return params;
}

export function hasActiveFilters(filters: RequestFilters): boolean {
  return Object.values(filters).some((v) => v != null);
}
