<template>
  <BoardLayout>
    <BoardHeader :schedule="schedule">
      <template v-if="$slots.region" #region>
        <slot name="region" />
      </template>
    </BoardHeader>

    <!-- one toolbar: the version switch (the page's, passed on), filters, zoom, editing -->
    <BoardControls>
      <template #versions>
        <slot name="versions" />
      </template>
    </BoardControls>

    <ScheduleEditBar />

    <TimelinePanel :region-name="schedule.region.displayName" />

    <!-- the requests left out are only known once the schedule is calculated (an earlier version shows
         the ones it left out); until then the crew's route takes the whole width -->
    <div
      class="grid grid-cols-1 items-start gap-4 sm:gap-6"
      :class="{ 'min-[901px]:grid-cols-[minmax(0,1fr)_minmax(0,1.25fr)]': showUnassigned }"
    >
      <CrewRoutePanel
        :crew="board.selectedCrew"
        :route="crewRoute"
        :historical="historical"
        :region-name="schedule.region.displayName"
      />
      <UnassignedRequestsPanel v-if="showUnassigned" :requests="board.unassigned" :target-date="schedule.targetDate" />
    </div>

    <VisitTooltip />
    <ScheduleEditMenu />
    <VisitStatusMenu />
    <CrewShiftMenu />
  </BoardLayout>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useBoardStore } from "../stores/board";
import type { CrewRoute, ScheduleData } from "../types";
import BoardControls from "./BoardControls.vue";
import BoardHeader from "./BoardHeader.vue";
import BoardLayout from "./BoardLayout.vue";
import CrewShiftMenu from "./CrewShiftMenu.vue";
import CrewRoutePanel from "./CrewRoutePanel.vue";
import ScheduleEditBar from "./ScheduleEditBar.vue";
import ScheduleEditMenu from "./ScheduleEditMenu.vue";
import TimelinePanel from "./TimelinePanel.vue";
import UnassignedRequestsPanel from "./UnassignedRequestsPanel.vue";
import VisitStatusMenu from "./VisitStatusMenu.vue";
import VisitTooltip from "./VisitTooltip.vue";

const props = defineProps<{
  schedule: ScheduleData;
  /** the selected crew's road route, loaded by the page; null while loading or when there is none */
  crewRoute: CrewRoute | null;
  /** `schedule` is an earlier version, shown read-only */
  historical?: boolean;
}>();

const board = useBoardStore();

// the server sends the unassigned list for a calculated schedule only — the current one, or an earlier
// (outdated) version with the requests it left out; while calculating it would wrongly read as "everything
// got a place"
const showUnassigned = computed(() => ["CREATED", "OUTDATED"].includes(props.schedule.status.keyword));
</script>
