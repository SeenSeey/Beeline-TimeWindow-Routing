<template>
  <!-- A link to the request; in edit mode a draggable handle that opens the edit menu instead.
       stop: the click/Enter must not reach the crew row, which toggles selection and prevents Enter's default -->
  <component
    :is="editing ? 'div' : RouterLink"
    v-bind="editing ? editAttrs : { to: { name: 'request', params: { id: visit.slot.requestId } } }"
    class="absolute top-1.5 bottom-1.5 min-w-[3px] overflow-hidden rounded-md text-[11px] leading-[1.25] text-white no-underline shadow-2xs hover:z-[1] hover:brightness-110 focus:outline-hidden focus-visible:z-[1] focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-1"
    :class="[
      { 'outline-2 outline-offset-1 outline-urgent outline-dashed': visit.urgent },
      editing ? 'cursor-grab active:cursor-grabbing' : '',
      { 'opacity-40': dragging },
      // a visit the edit placed where it breaks a rule
      { 'z-[1] ring-2 ring-danger ring-offset-2 ring-offset-surface': violations.length },
    ]"
    :data-visit="visitKey(visit)"
    :aria-label="ariaLabel"
    :style="{
      left: scale.left(from),
      width: scale.width(from, to),
      background: visit.color,
      opacity: dimmed && !dragging ? 0.25 : undefined,
    }"
    @click.stop="onClick"
    @keydown.enter.stop="onKeyboardOpen"
    @keydown.space.stop.prevent="onKeyboardOpen"
    @dragstart="onDragStart"
    @dragend="dragging = false"
  >
    <!-- Text only when the bar is wide enough to show something readable; the request number goes first
         so it survives truncation -->
    <div v-if="scale.px(from, to) >= MIN_TEXT_PX" class="pointer-events-none px-1.5 py-[3px]">
      <!-- right padding keeps the number clear of the vehicle icon -->
      <div class="mono truncate font-semibold" :class="{ 'pe-3.5': showVehicle }">#{{ visit.slot.requestId }}</div>
      <div class="mono truncate opacity-90">
        {{ formatTime(visit.slot.startSlotTime) }}–{{ formatTime(visit.slot.endSlotTime) }}
      </div>
      <div class="truncate opacity-90">{{ visit.slot.fullAddress }}</div>
    </div>

    <VehicleIcon
      v-if="showVehicle && visit.slot.requiredVehicle && !violations.length"
      :vehicle="visit.slot.requiredVehicle"
      class="pointer-events-none absolute top-[3px] end-[3px] size-3"
    />
    <!-- the rule violations are in the edit bar and in the hover tooltip -->
    <span
      v-if="violations.length"
      class="pointer-events-none absolute top-[3px] end-[3px] inline-flex size-3.5 items-center justify-center rounded-full bg-danger text-[9px] font-bold text-white"
      aria-hidden="true"
    >
      !
    </span>
  </component>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { RouterLink } from "vue-router";
import { formatTime } from "../model/format";
import { DRAG_REQUEST_TYPE, encodeDragPayload } from "../model/scheduleDraft";
import { visitKey, type TimeScale, type Visit } from "../model/timeline";
import { useScheduleEditStore } from "../stores/scheduleEdit";
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

const edit = useScheduleEditStore();
const editing = computed(() => edit.active);
const dragging = ref(false);

/** in edit mode the bar is a keyboard-reachable, draggable button-like handle */
const editAttrs = { role: "button", tabindex: 0, draggable: "true", "aria-haspopup": "menu" };

const showVehicle = computed(
  () => !!props.visit.slot.requiredVehicle && props.scale.px(props.from, props.to) >= MIN_ICON_PX,
);

/** what the edit broke for this visit; empty outside edit mode */
const violations = computed(() => (editing.value ? (edit.violationsByRequest.get(props.visit.slot.requestId) ?? []) : []));

const ariaLabel = computed(() => {
  const { requestId, fullAddress, requiredVehicle } = props.visit.slot;
  const vehicle = requiredVehicle ? `, нужен транспорт: ${requiredVehicle.displayName}` : "";
  const problems = violations.value.length ? `. Ошибки: ${violations.value.join("; ")}` : "";
  const action = editing.value ? ". Перетащите на строку бригады или откройте меню действий" : "";
  return `Заявка #${requestId}, ${fullAddress}${vehicle}${problems}${action}`;
});

function openMenuAt(x: number, y: number): void {
  edit.openMenu({ requestId: props.visit.slot.requestId, x, y });
}

function onClick(event: MouseEvent): void {
  if (editing.value) openMenuAt(event.clientX, event.clientY);
}

function onKeyboardOpen(event: KeyboardEvent): void {
  if (!editing.value) return;
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
  openMenuAt(rect.left, rect.bottom);
}

function onDragStart(event: DragEvent): void {
  if (!editing.value || !event.dataTransfer) return;
  // where in the bar it was grabbed, so the drop places the bar's start, not the cursor
  const bar = (event.currentTarget as HTMLElement).getBoundingClientRect();
  event.dataTransfer.setData(
    DRAG_REQUEST_TYPE,
    encodeDragPayload({ requestId: props.visit.slot.requestId, grabOffsetPx: event.clientX - bar.left }),
  );
  event.dataTransfer.effectAllowed = "move";
  edit.closeMenu();
  dragging.value = true;
}
</script>
