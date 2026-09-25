<template>
  <BoardLayout>
    <BoardHeader :schedule="schedule">
      <template v-if="$slots.region" #region>
        <slot name="region" />
      </template>
    </BoardHeader>

    <BoardControls />

    <ScheduleEditBar />

    <TimelinePanel :region-name="schedule.region.displayName" />

    <div class="grid grid-cols-1 items-start gap-4 sm:gap-6 min-[901px]:grid-cols-[minmax(0,1fr)_minmax(0,1.25fr)]">
      <CrewRoutePanel :crew="board.selectedCrew" :region-name="schedule.region.displayName" />
      <UnassignedRequestsPanel
        :requests="board.unassigned"
        :ready="schedule.status.keyword === 'CREATED'"
        :target-date="schedule.targetDate"
      />
    </div>

    <VisitTooltip />
    <ScheduleEditMenu />
    <VisitStatusMenu />
  </BoardLayout>
</template>

<script setup lang="ts">
import { useBoardStore } from "../stores/board";
import type { ScheduleData } from "../types";
import BoardControls from "./BoardControls.vue";
import BoardHeader from "./BoardHeader.vue";
import BoardLayout from "./BoardLayout.vue";
import CrewRoutePanel from "./CrewRoutePanel.vue";
import ScheduleEditBar from "./ScheduleEditBar.vue";
import ScheduleEditMenu from "./ScheduleEditMenu.vue";
import TimelinePanel from "./TimelinePanel.vue";
import UnassignedRequestsPanel from "./UnassignedRequestsPanel.vue";
import VisitStatusMenu from "./VisitStatusMenu.vue";
import VisitTooltip from "./VisitTooltip.vue";

defineProps<{
  schedule: ScheduleData;
}>();

const board = useBoardStore();
</script>
