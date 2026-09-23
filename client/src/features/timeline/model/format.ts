/** "HH:mm:ss" → minutes since midnight */
export function toMin(t: string): number {
  const [h = 0, m = 0] = t.split(":").map(Number);
  return h * 60 + m;
}

/** "HH:mm:ss" → "HH:mm" */
export const formatTime = (t?: string | null): string => (t ? t.slice(0, 5) : "—");

/** "YYYY-MM-DD" → "DD.MM.YYYY" */
export const formatDate = (d: string): string => d.split("-").reverse().join(".");

/** ISO LocalDateTime → "DD.MM.YYYY HH:mm" */
export const formatDateTime = (dt: string): string => `${formatDate(dt.slice(0, 10))} ${dt.slice(11, 16)}`;

/** 25 → "01:00" */
export const formatHour = (h: number): string => String(h % 24).padStart(2, "0") + ":00";

/** ISO LocalDateTime falls on the given "YYYY-MM-DD" */
export const isOnDate = (dateTime: string | null | undefined, date: string): boolean =>
  dateTime?.slice(0, 10) === date;
