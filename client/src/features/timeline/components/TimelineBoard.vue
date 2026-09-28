<template>
  <BoardLayout>
    <BoardHeader :schedule="schedule">
      <template v-if="$slots.actions" #actions>
        <slot name="actions" />
      </template>
      <template v-if="$slots.region" #region>
        <slot name="region" />
      </template>
    </BoardHeader>

    <BoardControls />

    <ScheduleEditBar />

    <TimelinePanel :region-name="schedule.region.displayName" />

    <!-- the requests left out are only known once the schedule is calculated; until then the crew's
         route takes the whole width -->
    <div
      class="grid grid-cols-1 items-start gap-4 sm:gap-6"
      :class="{ 'min-[901px]:grid-cols-[minmax(0,1fr)_minmax(0,1.25fr)]': !calculating }"
    >
      <CrewRoutePanel :crew="board.selectedCrew" :route="crewRoute" :region-name="schedule.region.displayName" />
      <UnassignedRequestsPanel v-if="!calculating" :requests="board.unassigned" :target-date="schedule.targetDate" />
    </div>

    <VisitTooltip />
    <ScheduleEditMenu />
    <VisitStatusMenu />
    <CrewShiftMenu />
  </BoardLayout>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { isScheduleCalculating } from "../model/scheduleStatus";
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
}>();

const board = useBoardStore();

const calculating = computed(() => isScheduleCalculating(props.schedule.status.keyword));
</script>
