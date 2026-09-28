import { defineStore } from "pinia";
import { ref, shallowRef } from "vue";
import { createRequest, fetchRegions, toApiFailure } from "../api/requestsApi";
import type { RegionInfo, RequestCreateBody, RequestItem } from "../types";

/**
 * The "Создать заявку" dialog, opened from the app header on any page. It loads the regions it offers
 * itself (not through the schedule's regions store, which writes ?region= into the URL — a filter on
 * other pages), and announces each created request in `lastCreated` for the pages to refresh.
 */
export const useRequestCreateStore = defineStore("requests/create", () => {
  const open = ref(false);
  /** the request is being created */
  const pending = ref(false);
  /** why the last attempt failed; cleared when the dialog is opened again or on the next try */
  const error = ref<string | null>(null);
  /** the regions to choose from, loaded when the dialog is first opened */
  const regions = ref<RegionInfo[]>([]);
  /** the last request created from the dialog; pages showing its region reload when it changes */
  const lastCreated = shallowRef<RequestItem | null>(null);

  async function loadRegions(): Promise<void> {
    if (regions.value.length) return;
    try {
      regions.value = await fetchRegions();
    } catch (e) {
      const failure = toApiFailure(e);
      error.value = failure.message ?? "Не удалось загрузить регионы";
    }
  }

  function openDialog(): void {
    open.value = true;
    error.value = null;
    void loadRegions();
  }

  function closeDialog(): void {
    if (pending.value) return;
    open.value = false;
    error.value = null;
  }

  /** Creates the request and closes the dialog; null (the dialog stays open with the reason) on failure */
  async function create(body: RequestCreateBody): Promise<RequestItem | null> {
    if (pending.value) return null;
    pending.value = true;
    error.value = null;
    try {
      const created = await createRequest(body);
      pending.value = false;
      open.value = false;
      lastCreated.value = created;
      return created;
    } catch (e) {
      const failure = toApiFailure(e);
      error.value = failure.message ?? `Не удалось создать заявку${failure.status ? ` (HTTP ${failure.status})` : ""}`;
      pending.value = false;
      return null;
    }
  }

  return { open, pending, error, regions, lastCreated, openDialog, closeDialog, create };
});
