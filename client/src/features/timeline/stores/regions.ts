import { defineStore } from "pinia";
import { ref } from "vue";
import { fetchRegions } from "../api/timelineApi";
import type { RegionInfo } from "../types";

// The chosen region lives in the URL (?region=2), so reloads and shared links keep it
function readRegionFromUrl(): number | null {
  const id = Number(new URLSearchParams(window.location.search).get("region"));
  return Number.isInteger(id) && id > 0 ? id : null;
}
function writeRegionToUrl(id: number): void {
  const url = new URL(window.location.href);
  url.searchParams.set("region", String(id));
  window.history.replaceState(null, "", url);
}

export const useRegionsStore = defineStore("timeline/regions", () => {
  const regions = ref<RegionInfo[]>([]);
  const regionId = ref<number | null>(readRegionFromUrl());

  /** Loads the list and settles on a valid region (the URL's one, else the first). Throws on API errors. */
  async function load(): Promise<void> {
    // a link may have brought a different ?region= since the store was created
    regionId.value = readRegionFromUrl() ?? regionId.value;
    regions.value = await fetchRegions();
    if (!regions.value.some((r) => r.id === regionId.value)) regionId.value = regions.value[0]?.id ?? null;
    if (regionId.value != null) writeRegionToUrl(regionId.value);
  }

  function select(id: number): void {
    regionId.value = id;
    writeRegionToUrl(id);
  }

  return { regions, regionId, load, select };
});
