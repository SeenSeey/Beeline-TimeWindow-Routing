import { computed } from "vue";
import { useRoute } from "vue-router";

export type NavIconName = "map" | "calendar" | "list" | "users" | "settings";

/** App sections; the sidebar (large screens) and the header (small screens) both render these */
export const NAV_LINKS: { to: string; label: string; icon: NavIconName }[] = [
  { to: "/map", label: "Карта", icon: "map" },
  { to: "/schedule", label: "Расписание", icon: "calendar" },
  { to: "/requests", label: "Заявки", icon: "list" },
  { to: "/crews", label: "Исполнители", icon: "users" },
  { to: "/system", label: "Система", icon: "settings" },
];

export function useActiveSection() {
  const route = useRoute();

  /** A section stays active on its nested pages too (/requests/42 belongs to /requests) */
  const isActive = (sectionPath: string): boolean =>
    route.path === sectionPath || route.path.startsWith(`${sectionPath}/`);

  const activeLink = computed(() => NAV_LINKS.find((link) => isActive(link.to)) ?? null);

  return { isActive, activeLink };
}
