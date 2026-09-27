import type { JobType, JobTypeKeyword, ScheduleStatusKeyword, SkillKeyword } from "../types";

// Styles are keyed by server enum keywords; display names are only ever shown, never matched.

const JOB_COLORS: Record<JobTypeKeyword, string> = {
  INSTALLATION: "var(--job-installation)",
  LOCAL_WORK: "var(--job-local)",
  ADDON: "var(--job-addon)",
  EMERGENCY: "var(--job-emergency)",
};

/** unknown keywords (a server enum grew) stay visible in a neutral colour */
export const jobTypeColor = (keyword: JobTypeKeyword): string => JOB_COLORS[keyword] ?? "var(--faint)";

/** each skill in the colour of the job types it covers */
const SKILL_COLORS: Record<SkillKeyword, string> = {
  LOCAL_WORK: "var(--job-local)",
  INSTALLATION_OR_ADDON: "var(--job-installation)",
  EMERGENCY: "var(--job-emergency)",
};

export const skillColor = (keyword: SkillKeyword): string => SKILL_COLORS[keyword] ?? "var(--muted)";

export interface JobTypeOption extends JobType {
  color: string;
}

/** Distinct job types in the mockup's legend order */
export function collectJobTypes(jobTypes: JobType[]): JobTypeOption[] {
  const order = Object.keys(JOB_COLORS);
  const byKeyword = new Map(jobTypes.map((j) => [j.keyword, j]));
  return [...byKeyword.values()]
    .sort((a, b) => order.indexOf(a.keyword) - order.indexOf(b.keyword))
    .map((j) => ({ ...j, color: jobTypeColor(j.keyword) }));
}

const SCHEDULE_STATUS_COLORS: Record<ScheduleStatusKeyword, string> = {
  CREATED: "var(--job-local)",
  CALCULATING: "var(--job-addon)",
  RECALCULATING: "var(--job-addon)",
};

export const scheduleStatusColor = (keyword: ScheduleStatusKeyword): string =>
  SCHEDULE_STATUS_COLORS[keyword] ?? "var(--muted)";
