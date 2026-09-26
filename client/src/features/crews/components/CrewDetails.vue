<template>
  <!-- same page frame as the request details -->
  <div class="grid grid-cols-[minmax(0,1fr)] content-start gap-4 p-4 sm:gap-6 sm:p-6">
    <!-- 1. who: name and number -->
    <header class="grid gap-2">
      <RouterLink
        :to="{ name: 'crews' }"
        class="inline-flex w-fit items-center gap-x-1.5 rounded-sm text-sm text-muted no-underline hover:text-accent focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
      >
        <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <path d="m15 18-6-6 6-6" />
        </svg>
        Все исполнители
      </RouterLink>

      <div class="flex flex-wrap items-center gap-x-3 gap-y-2">
        <h1 class="m-0 text-2xl font-semibold text-balance text-ink">{{ crew.name }}</h1>
        <span class="mono text-sm text-muted">№{{ crew.id }}</span>
      </div>
    </header>

    <!-- 2. at a glance -->
    <CrewKeyFacts :crew="crew" />

    <!-- 3. today's work and the request history on the left, capabilities on the side -->
    <div class="grid grid-cols-1 items-start gap-4 sm:gap-6 xl:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
      <div class="grid min-w-0 gap-4 sm:gap-6">
        <CrewRoutePanel :visits="crew.route" :route-date="crew.routeDate" :region-id="crew.region.id" />
        <CrewRequestsPanel :requests="crew.assignedRequests" />
      </div>

      <UiPanel title="Навыки и оснащение">
        <dl class="m-0 divide-y divide-line px-5">
          <div v-for="group in capabilityGroups" :key="group.label" class="grid gap-2 py-3 text-sm">
            <dt class="text-muted">{{ group.label }}</dt>
            <dd class="m-0 flex min-w-0 flex-wrap gap-1.5">
              <template v-if="group.items.length">
                <UiBadge v-for="item in group.items" :key="item.label" :color="item.color">{{ item.label }}</UiBadge>
              </template>
              <span v-else class="text-faint">—</span>
            </dd>
          </div>
        </dl>
      </UiPanel>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiBadge from "@/components/ui/UiBadge.vue";
import UiPanel from "@/components/ui/UiPanel.vue";
import type { CrewDetailed, SkillKeyword } from "../types";
import CrewKeyFacts from "./CrewKeyFacts.vue";
import CrewRequestsPanel from "./CrewRequestsPanel.vue";
import CrewRoutePanel from "./CrewRoutePanel.vue";

const props = defineProps<{
  crew: CrewDetailed;
}>();

/** each skill in the colour of the job types it covers (as in the crews table) */
const SKILL_COLORS: Record<SkillKeyword, string> = {
  LOCAL_WORK: "var(--job-local)",
  INSTALLATION_OR_ADDON: "var(--job-installation)",
  EMERGENCY: "var(--job-emergency)",
};

const capabilityGroups = computed(() => [
  {
    label: "Навыки",
    items: props.crew.skills.map((skill) => ({
      label: skill.displayName,
      color: SKILL_COLORS[skill.keyword] ?? "var(--muted)",
    })),
  },
  {
    label: "Оборудование",
    items: props.crew.equipment.map((item) => ({
      label: `${item.displayName} × ${item.quantity}`,
      color: "var(--muted)",
    })),
  },
  {
    label: "Транспорт",
    items: props.crew.vehicle ? [{ label: props.crew.vehicle.displayName, color: "var(--accent)" }] : [],
  },
]);
</script>
