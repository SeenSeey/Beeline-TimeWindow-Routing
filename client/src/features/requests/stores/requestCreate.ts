import { defineStore } from "pinia";
import { ref } from "vue";
import { createRequest, toApiFailure } from "../api/requestsApi";
import type { RequestCreateBody, RequestItem } from "../types";

/** The "Создать заявку" dialog, shared by the pages that offer it (schedule board, map) */
export const useRequestCreateStore = defineStore("requests/create", () => {
  const open = ref(false);
  /** the request is being created */
  const pending = ref(false);
  /** why the last attempt failed; cleared when the dialog is opened again or on the next try */
  const error = ref<string | null>(null);

  function openDialog(): void {
    open.value = true;
    error.value = null;
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
      return created;
    } catch (e) {
      const failure = toApiFailure(e);
      error.value = failure.message ?? `Не удалось создать заявку${failure.status ? ` (HTTP ${failure.status})` : ""}`;
      pending.value = false;
      return null;
    }
  }

  return { open, pending, error, openDialog, closeDialog, create };
});
