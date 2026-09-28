<template>
  <div ref="scroller" class="max-w-full overflow-x-auto" @wheel="onWheel" @scroll="onScroll">
    <div class="relative" :style="{ width: labelWidth + trackWidth + 'px' }">
      <TimelineAxis :hours="board.axis.hours" :scale="scale" :scrolled="scrolled" :label-width="labelWidth" />

      <div class="border-b border-line px-5 py-2.5 text-sm font-semibold text-ink">
        <div class="sticky left-5 flex w-fit items-center gap-2">
          <UiDot color="var(--accent)" />{{ regionName }}
          <small class="text-xs font-normal text-muted">
            {{ board.visibleCrews.length }} бригад · {{ visibleRequestCount }} заявок
          </small>
        </div>
      </div>

      <TimelineRow
        v-for="c in board.visibleCrews"
        :key="c.id"
        :crew="c"
        :hours="board.axis.hours"
        :scale="scale"
        :selected="board.selectedCrewId === c.id"
        :scrolled="scrolled"
        :label-width="labelWidth"
        :job-type-filter="board.jobType"
        @select="board.toggleCrew(c.id)"
      />

      <UiEmptyState v-if="!board.visibleCrews.length">Нет бригад под выбранный фильтр.</UiEmptyState>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import UiDot from "@/components/ui/UiDot.vue";
import UiEmptyState from "@/components/ui/UiEmptyState.vue";
import { createScale } from "../model/timeline";
import { useBoardStore } from "../stores/board";
import { useScheduleEditStore } from "../stores/scheduleEdit";
import TimelineAxis from "./TimelineAxis.vue";
import TimelineRow from "./TimelineRow.vue";

defineProps<{
  regionName: string;
}>();

/** at 100% the track fills the visible width but keeps at least this many px per hour */
const MIN_HOUR_PX = 58;
/** the sticky crew column: at least this wide, wider when a crew's label needs it (its text is never cut) */
const MIN_LABEL_WIDTH = 230;
/** the label cell's padding around its text (ps-5 pe-3 in TimelineRow) */
const LABEL_PADDING_PX = 32;

const board = useBoardStore();
const edit = useScheduleEditStore();

const scroller = ref<HTMLElement | null>(null);

// ── The crew column fits the widest label ──
/** px of the crew column, shared by the axis and every row */
const labelWidth = ref(MIN_LABEL_WIDTH);

/**
 * Each row marks its never-cut lines (name, vehicle and route, shift) with data-crew-label, sized to their
 * text: the column takes the widest of them. Measured at their own width, not the column's, so it narrows
 * back when the widest crew is filtered out
 */
function measureLabels(): void {
  const labels = scroller.value?.querySelectorAll<HTMLElement>("[data-crew-label]") ?? [];
  let widest = 0;
  labels.forEach((label) => (widest = Math.max(widest, label.offsetWidth)));
  labelWidth.value = Math.max(MIN_LABEL_WIDTH, Math.ceil(widest) + LABEL_PADDING_PX);
}

// again whenever the labels may have changed: other crews shown, a new schedule, edit mode (its badges)
watch(
  () => [board.visibleCrews, edit.active],
  async () => {
    await nextTick();
    measureLabels();
  },
  { flush: "post" },
);
const viewportWidth = ref(0);
const scrolled = ref(false);

const baseWidth = computed(() => {
  const hours = (board.axis.end - board.axis.origin) / 60;
  return Math.max(viewportWidth.value - labelWidth.value, hours * MIN_HOUR_PX);
});
const trackWidth = computed(() => Math.round(baseWidth.value * board.zoom));
const scale = computed(() => createScale(board.axis, trackWidth.value));

const visibleRequestCount = computed(() => board.visibleCrews.reduce((n, c) => n + c.requests.length, 0));

// ── Zoom keeps the anchored time in place: the cursor for wheel zoom, the middle otherwise ──
let wheelAnchorX: number | null = null;
// Scroll target not yet applied to the DOM (several wheel events can land before a re-render)
let pendingScrollLeft: number | null = null;

watch(
  () => board.zoom,
  async (zoom, prevZoom) => {
    const el = scroller.value;
    if (!el) return;
    const x = wheelAnchorX ?? labelWidth.value + (el.clientWidth - labelWidth.value) / 2;
    const ratio = ((pendingScrollLeft ?? el.scrollLeft) + x - labelWidth.value) / (baseWidth.value * prevZoom);
    pendingScrollLeft = ratio * baseWidth.value * zoom + labelWidth.value - x;

    await nextTick();
    if (pendingScrollLeft != null) {
      el.scrollLeft = pendingScrollLeft;
      pendingScrollLeft = null;
    }
  },
  // sync: runs inside setZoom, while wheelAnchorX is still set
  { flush: "sync" },
);

// Ctrl/⌘ + wheel (and trackpad pinch) zooms smoothly around the cursor
function onWheel(e: WheelEvent): void {
  if (!(e.ctrlKey || e.metaKey) || !scroller.value) return;
  e.preventDefault();
  const x = e.clientX - scroller.value.getBoundingClientRect().left;
  if (x < labelWidth.value) return;
  wheelAnchorX = x;
  board.setZoom(board.zoom * Math.exp(-e.deltaY * 0.005));
  wheelAnchorX = null;
}

function onScroll(): void {
  scrolled.value = (scroller.value?.scrollLeft ?? 0) > 0;
}

let resizeObserver: ResizeObserver | null = null;
onMounted(() => {
  if (!scroller.value) return;
  measureLabels();
  resizeObserver = new ResizeObserver(([entry]) => {
    viewportWidth.value = entry?.contentRect.width ?? 0;
  });
  resizeObserver.observe(scroller.value);
});
onBeforeUnmount(() => resizeObserver?.disconnect());
</script>
