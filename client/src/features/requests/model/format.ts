export { formatDate, formatDateTime, formatTime } from "@/features/timeline/model/format";
import { formatDateTime } from "@/features/timeline/model/format";

/** 90 → "1 ч 30 мин" */
export function formatDuration(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (!h) return `${m} мин`;
  return m ? `${h} ч ${m} мин` : `${h} ч`;
}

/** ISO LocalDateTime or null → "DD.MM.YYYY HH:mm" or null (rendered as a dash) */
export const formatOptionalDateTime = (dt: string | null): string | null => (dt ? formatDateTime(dt) : null);

/** Yandex Maps link with a pin at the point */
export const mapUrl = (latitude: number, longitude: number): string =>
  `https://yandex.ru/maps/?pt=${longitude},${latitude}&z=17&l=map`;
