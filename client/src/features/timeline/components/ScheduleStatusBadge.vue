<template>
  <UiBadge size="md" dot :color="scheduleStatusColor(status.keyword)" role="status">{{ status.displayName }}</UiBadge>
</template>

<script setup lang="ts">
import UiBadge from "@/components/ui/UiBadge.vue";
import { scheduleStatusColor } from "../model/colors";
import type { ScheduleStatus } from "../types";

defineProps<{
  status: ScheduleStatus;
}>();
</script>
