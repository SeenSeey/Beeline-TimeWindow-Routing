import { defineStore } from "pinia";
import { computed, ref } from "vue";
import router from "@/router";
import { fetchRegions, fetchRequests, toApiFailure, type ApiFailure } from "../api/requestsApi";
import { EMPTY_FILTERS, readQuery, toUrlQuery, type RequestsQuery } from "../model/query";
import type { PageResponse, RegionInfo, RequestFilters, RequestItem } from "../types";

export type RequestsLoadState = { kind: "loading" } | { kind: "ready"; data: PageResponse<RequestItem> } | ApiFailure;

export const useRequestsStore = defineStore("requests/list", () => {
  const query = ref<RequestsQuery>(readQuery(new URLSearchParams(window.location.search)));
  const state = ref<RequestsLoadState>({ kind: "loading" });
  /** a new page or filter set is loading; the current rows stay visible meanwhile */
  const switching = ref(false);
  /** options of the region filter; the filter still works by URL when they fail to load */
  const regions = ref<RegionInfo[]>([]);
  // Only the latest request may update the state (fast paging / filter changes)
  let requestSeq = 0;

  const page = computed(() => (state.value.kind === "ready" ? state.value.data : null));

  async function load(): Promise<void> {
    const seq = ++requestSeq;
    switching.value = state.value.kind === "ready";
    void router.replace({ query: toUrlQuery(query.value) });

    let next: RequestsLoadState;
    try {
      next = { kind: "ready", data: await fetchRequests(query.value) };
    } catch (e) {
      next = toApiFailure(e);
    }
    if (seq !== requestSeq) return;

    // The page asked for is past the end (e.g. from an old link): jump to the last one
    if (next.kind === "ready" && next.data.totalPages > 0 && query.value.page > next.data.totalPages) {
      query.value = { ...query.value, page: next.data.totalPages };
      return load();
    }
    state.value = next;
    switching.value = false;
  }

  async function loadRegions(): Promise<void> {
    try {
      regions.value = await fetchRegions();
    } catch (e) {
      toApiFailure(e); // logs it; the list stays usable without region names
    }
  }

  /** Initial load: the list and the filter options in parallel */
  async function init(): Promise<void> {
    state.value = { kind: "loading" };
    await Promise.all([load(), loadRegions()]);
  }

  /** Any filter change starts over from the first page */
  function setFilters(filters: RequestFilters): void {
    query.value = { ...query.value, filters, page: 1 };
    void load();
  }

  const resetFilters = (): void => setFilters({ ...EMPTY_FILTERS });

  function setPage(value: number): void {
    if (value === query.value.page) return;
    query.value = { ...query.value, page: value };
    void load();
  }

  function setLimit(limit: number): void {
    query.value = { ...query.value, limit, page: 1 };
    void load();
  }

  return { query, state, switching, regions, page, init, load, setFilters, resetFilters, setPage, setLimit };
});
