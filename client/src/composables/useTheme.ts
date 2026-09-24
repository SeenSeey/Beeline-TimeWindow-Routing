import { computed, ref } from "vue";

// global.css follows the system colour scheme unless <html data-theme="light|dark"> overrides it.
// The viewer's explicit choice is remembered in this browser; without one the system scheme applies.

export type Theme = "light" | "dark";

const STORAGE_KEY = "theme";
const darkQuery = window.matchMedia("(prefers-color-scheme: dark)");

function readStoredTheme(): Theme | null {
  try {
    const value = localStorage.getItem(STORAGE_KEY);
    return value === "light" || value === "dark" ? value : null;
  } catch {
    return null;
  }
}

/** the explicit choice; null follows the system */
const chosenTheme = ref<Theme | null>(readStoredTheme());
const systemPrefersDark = ref(darkQuery.matches);
darkQuery.addEventListener("change", (event) => (systemPrefersDark.value = event.matches));

function applyTheme(theme: Theme | null): void {
  if (theme) document.documentElement.dataset.theme = theme;
  else delete document.documentElement.dataset.theme;
}

/** Call once before mounting the app, so the page never flashes the other theme */
export function applyStoredTheme(): void {
  applyTheme(chosenTheme.value);
}

export function useTheme() {
  const activeTheme = computed<Theme>(() => chosenTheme.value ?? (systemPrefersDark.value ? "dark" : "light"));

  function toggleTheme(): void {
    const nextTheme: Theme = activeTheme.value === "dark" ? "light" : "dark";
    chosenTheme.value = nextTheme;
    applyTheme(nextTheme);
    try {
      localStorage.setItem(STORAGE_KEY, nextTheme);
    } catch {
      /* storage unavailable: the choice lasts for this page load */
    }
  }

  return { activeTheme, toggleTheme };
}
