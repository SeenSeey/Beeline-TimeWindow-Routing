<template>
  <!-- A link to the request; in edit mode a draggable handle that opens the edit menu instead.
       stop: the click/Enter must not reach the crew row, which toggles selection and prevents Enter's default.
       Looks, in class order:
       - due today: dashed urgent outline, only while the request is still open;
       - canceled: a light grey fill (the slot-canceled token) with a thin grey border and muted text,
         whatever the job type;
       - completed: faded, done with;
       - in progress: a white inner ring, raised above its neighbours;
       - edit mode: grab cursor, dimmed while dragged, and a red ring where the edit breaks a rule.
       (No comments inside the bindings below: a quote in one would end the HTML attribute.) -->
  <component
    :is="editing ? 'div' : RouterLink"
    v-bind="editing ? editAttrs : { to: { name: 'request', params: { id: visit.slot.requestId } } }"
    class="absolute top-1.5 bottom-1.5 min-w-[3px] overflow-hidden rounded-md text-[11px] leading-[1.25] text-white no-underline shadow-2xs hover:z-[1] hover:brightness-110 focus:outline-hidden focus-visible:z-[1] focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-1"
    :class="[
      {
        'outline-2 outline-offset-1 outline-urgent outline-dashed':
          visit.urgent && progress !== 'canceled' && progress !== 'completed',
      },
      { 'border border-line-strong text-muted! shadow-none!': progress === 'canceled' },
      { 'opacity-55 saturate-50 shadow-none!': progress === 'completed' },
      { 'z-[1]': progress === 'inProgress' },
      { 'ring-2 ring-white ring-inset': progress === 'inProgress' && !violations.length },
      editing ? 'cursor-grab active:cursor-grabbing' : '',
      { 'opacity-40': dragging },
      { 'z-[1] ring-2 ring-danger ring-offset-2 ring-offset-surface': violations.length },
    ]"
    :data-visit="visitKey(visit)"
    :aria-label="ariaLabel"
    :style="{
      left: scale.left(from),
      width: scale.width(from, to),
      backgroundColor: progress === 'canceled' ? 'var(--slot-canceled)' : visit.color,
      opacity: dimmed && !dragging ? 0.25 : undefined,
    }"
    @click.stop="onClick"
    @contextmenu.prevent.stop="onContextMenu"
    @keydown.enter.stop="onKeyboardOpen"
    @keydown.space.stop.prevent="onKeyboardOpen"
    @dragstart="onDragStart"
    @dragend="dragging = false"
  >
    <!-- Text only when the bar is wide enough to show something readable; the request number goes first
         so it survives truncation -->
    <div v-if="scale.px(from, to) >= MIN_TEXT_PX" class="pointer-events-none px-1.5 py-[3px]">
      <!-- right padding keeps the number clear of the vehicle icon -->
      <div
        class="mono flex items-center gap-x-1 truncate font-semibold"
        :class="{ 'pe-3.5': showVehicle }"
      >
        <svg
          v-if="progress === 'completed'"
          class="size-2.5 shrink-0"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="4"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M20 6 9 17l-5-5" />
        </svg>
        #{{ visit.slot.requestId }}
      </div>
      <div class="mono truncate opacity-90">
        {{ formatTime(visit.slot.startSlotTime) }}–{{ formatTime(visit.slot.endSlotTime) }}
      </div>
      <!-- in progress and canceled: the status replaces the address; completed ones keep the address,
           their faded look and check mark say enough -->
      <div
        v-if="progress === 'inProgress'"
        class="inline-flex max-w-full items-center gap-x-1 truncate rounded-sm bg-white px-1 font-bold tracking-wide uppercase"
        :style="{ color: visit.color }"
      >
        <UiDot size="sm" pulse />
        {{ PROGRESS_LABELS.inProgress }}
      </div>
      <div v-else-if="progress === 'canceled'" class="truncate">
        {{ PROGRESS_LABELS.canceled }}
      </div>
      <div v-else class="truncate opacity-90">{{ visit.slot.fullAddress }}</div>
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
import { useVisitStatusStore } from "../stores/visitStatus";
import UiDot from "@/components/ui/UiDot.vue";
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
const statusStore = useVisitStatusStore();
const editing = computed(() => edit.active);
const dragging = ref(false);

/** in edit mode the bar is a keyboard-reachable, draggable button-like handle */
const editAttrs = { role: "button", tabindex: 0, draggable: "true", "aria-haspopup": "menu" };

const showVehicle = computed(
  () => !!props.visit.slot.requiredVehicle && props.scale.px(props.from, props.to) >= MIN_ICON_PX,
);

type Progress = "inProgress" | "completed" | "canceled";

const PROGRESS_LABELS: Record<Progress, string> = {
  inProgress: "в работе",
  completed: "выполнена",
  canceled: "отменена",
};

/** how far the request got since the schedule was built; null while it's just planned */
const progress = computed<Progress | null>(() => {
  const { status, canceledAt, completedAt } = props.visit.slot;
  if (status?.keyword === "CANCELED" || canceledAt != null) return "canceled";
  if (status?.keyword === "COMPLETED" || completedAt != null) return "completed";
  if (status?.keyword === "IN_PROGRESS") return "inProgress";
  return null;
});

/** what the edit broke for this visit; empty outside edit mode */
const violations = computed(() => (editing.value ? (edit.violationsByRequest.get(props.visit.slot.requestId) ?? []) : []));

const ariaLabel = computed(() => {
  const { requestId, fullAddress, requiredVehicle, requiredEquipment } = props.visit.slot;
  const vehicle = requiredVehicle ? `, нужен транспорт: ${requiredVehicle.displayName}` : "";
  const equipment = requiredEquipment ? `, нужно оборудование: ${requiredEquipment}` : "";
  const problems = violations.value.length ? `. Ошибки: ${violations.value.join("; ")}` : "";
  const state = progress.value ? `, ${PROGRESS_LABELS[progress.value]}` : "";
  const action = editing.value ? ". Перетащите на строку бригады или откройте меню действий" : "";
  return `Заявка #${requestId}${state}, ${fullAddress}${vehicle}${equipment}${problems}${action}`;
});

function openMenuAt(x: number, y: number): void {
  edit.openMenu({ requestId: props.visit.slot.requestId, x, y });
}

function onClick(event: MouseEvent): void {
  if (editing.value) openMenuAt(event.clientX, event.clientY);
}

/**
 * Right-click (or the keyboard's context-menu key, Shift+F10, a long press on touch): the status menu —
 * start work, complete, cancel. In edit mode it opens the edit menu instead: a status change would clash
 * with the unsaved draft.
 */
function onContextMenu(event: MouseEvent): void {
  if (editing.value) {
    openMenuAt(event.clientX, event.clientY);
    return;
  }
  // a keyboard-opened context menu reports no pointer position: place it under the bar
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
  const fromKeyboard = event.clientX === 0 && event.clientY === 0;
  statusStore.openMenu({
    requestId: props.visit.slot.requestId,
    x: fromKeyboard ? rect.left : event.clientX,
    y: fromKeyboard ? rect.bottom : event.clientY,
  });
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
