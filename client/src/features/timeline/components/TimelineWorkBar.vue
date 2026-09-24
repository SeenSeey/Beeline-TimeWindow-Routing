<template>
  <!-- stop: the click/Enter must not reach the crew row, which toggles selection and prevents Enter's default -->
  <RouterLink
    :to="{ name: 'request', params: { id: visit.slot.requestId } }"
    class="absolute top-1.5 bottom-1.5 min-w-[3px] overflow-hidden rounded-md text-[11px] leading-[1.25] text-white no-underline shadow-2xs hover:z-[1] hover:brightness-110 focus:outline-hidden focus-visible:z-[1] focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-1"
    :class="{ 'outline-2 outline-offset-1 outline-urgent outline-dashed': visit.urgent }"
    :data-visit="visitKey(visit)"
    :aria-label="ariaLabel"
    :style="{
      left: scale.left(from),
      width: scale.width(from, to),
      background: visit.color,
      opacity: dimmed ? 0.25 : undefined,
    }"
    @click.stop
    @keydown.enter.stop
  >
    <!-- Text only when the bar is wide enough to show something readable; the request number goes first
         so it survives truncation -->
    <div v-if="scale.px(from, to) >= MIN_TEXT_PX" class="px-1.5 py-[3px]">
      <!-- right padding keeps the number clear of the vehicle icon -->
      <div class="mono truncate font-semibold" :class="{ 'pe-3.5': showVehicle }">#{{ visit.slot.requestId }}</div>
      <div class="mono truncate opacity-90">
        {{ formatTime(visit.slot.startSlotTime) }}–{{ formatTime(visit.slot.endSlotTime) }}
      </div>
      <div class="truncate opacity-90">{{ visit.slot.fullAddress }}</div>
    </div>

    <VehicleIcon
      v-if="showVehicle && visit.slot.requiredVehicle"
      :vehicle="visit.slot.requiredVehicle"
      class="pointer-events-none absolute top-[3px] end-[3px] size-3"
    />
  </RouterLink>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { formatTime } from "../model/format";
import { visitKey, type TimeScale, type Visit } from "../model/timeline";
import VehicleIcon from "./VehicleIcon.vue";

const MIN_TEXT_PX = 36;
/** the icon alone still fits a bar too narrow for text */
const MIN_ICON_PX = 18;

const props = defineProps<{
  visit: Visit;
  /** axis minutes */
  from: number;
  to: number;
  scale: TimeScale;
  /** filtered out by the job type filter */
  dimmed?: boolean;
}>();

const showVehicle = computed(
  () => !!props.visit.slot.requiredVehicle && props.scale.px(props.from, props.to) >= MIN_ICON_PX,
);

const ariaLabel = computed(() => {
  const { requestId, fullAddress, requiredVehicle } = props.visit.slot;
  const vehicle = requiredVehicle ? `, нужен транспорт: ${requiredVehicle.displayName}` : "";
  return `Заявка #${requestId}, ${fullAddress}${vehicle}`;
});
</script>
