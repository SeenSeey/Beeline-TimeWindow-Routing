// Basemap for the MapLibre maps: OpenFreeMap's light Positron style, free and without an API key.
// Light in both themes — the markers and the route line stay readable on it.
const MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/positron";

/** Style URL for a new map */
export const currentMapStyleUrl = (): string => MAP_STYLE_URL;
