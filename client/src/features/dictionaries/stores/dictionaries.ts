import { defineStore } from "pinia";
import { ref, type Ref } from "vue";
import { fetchDictionary, toApiFailure, type DictionaryName } from "../api/dictionariesApi";
import type { DictionaryItem } from "../types";

/**
 * The dictionaries behind every selector and filter — request statuses, job types, vehicles, skills,
 * equipment — loaded from the server, never written into the client: their ids depend on the database.
 * Each is loaded once per session; pages ask for the ones they offer (load), components read the lists.
 */
export const useDictionariesStore = defineStore("dictionaries", () => {
  const requestStatuses = ref<DictionaryItem[]>([]);
  const jobTypes = ref<DictionaryItem[]>([]);
  const vehicles = ref<DictionaryItem[]>([]);
  const skills = ref<DictionaryItem[]>([]);
  const equipment = ref<DictionaryItem[]>([]);

  const lists: Record<DictionaryName, Ref<DictionaryItem[]>> = {
    "request-statuses": requestStatuses,
    "job-types": jobTypes,
    vehicles,
    skills,
    equipment,
  };

  /** loaded, or loading: a dictionary is fetched once, however many pages ask for it */
  const requests = new Map<DictionaryName, Promise<void>>();

  function loadOne(name: DictionaryName): Promise<void> {
    const inFlight = requests.get(name);
    if (inFlight) return inFlight;

    const request = fetchDictionary(name)
      .then((items) => {
        lists[name].value = items;
      })
      .catch((e) => {
        // logged; the selector stays empty, and the next page to ask tries again
        toApiFailure(e);
        requests.delete(name);
      });
    requests.set(name, request);
    return request;
  }

  /** Loads the given dictionaries (all by default), in parallel; failures leave that list empty */
  async function load(names: DictionaryName[] = Object.keys(lists) as DictionaryName[]): Promise<void> {
    await Promise.all(names.map(loadOne));
  }

  return { requestStatuses, jobTypes, vehicles, skills, equipment, load };
});
