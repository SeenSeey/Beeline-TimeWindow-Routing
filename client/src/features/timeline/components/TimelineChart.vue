<template>
  <div ref="scroller" class="max-w-full overflow-x-auto" @wheel="onWheel" @scroll="onScroll">
    <div class="relative" :style="{ width: LABEL_WIDTH + trackWidth + 'px' }">
      <TimelineAxis :hours="board.axis.hours" :scale="scale" :scrolled="scrolled" />

      <div class="border-t border-line px-4 pt-2.5 pb-1 text-[13px] font-semibold">
        <div class="sticky left-4 flex w-fit items-center gap-2">
          <UiDot color="var(--accent)" />{{ regionName }}
          <small class="font-normal text-muted">
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
import TimelineAxis from "./TimelineAxis.vue";
import TimelineRow from "./TimelineRow.vue";

defineProps<{
  regionName: string;
}>();

/** width of the sticky crew column; matches grid-cols-[230px_1fr] in the axis and rows */
const LABEL_WIDTH = 230;
/** at 100% the track fills the visible width but keeps at least this many px per hour */
const MIN_HOUR_PX = 58;

const board = useBoardStore();

const scroller = ref<HTMLElement | null>(null);
const viewportWidth = ref(0);
const scrolled = ref(false);

const baseWidth = computed(() => {
  const hours = (board.axis.end - board.axis.origin) / 60;
  return Math.max(viewportWidth.value - LABEL_WIDTH, hours * MIN_HOUR_PX);
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
    const x = wheelAnchorX ?? LABEL_WIDTH + (el.clientWidth - LABEL_WIDTH) / 2;
    const ratio = ((pendingScrollLeft ?? el.scrollLeft) + x - LABEL_WIDTH) / (baseWidth.value * prevZoom);
    pendingScrollLeft = ratio * baseWidth.value * zoom + LABEL_WIDTH - x;

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
  if (x < LABEL_WIDTH) return;
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
  resizeObserver = new ResizeObserver(([entry]) => {
    viewportWidth.value = entry?.contentRect.width ?? 0;
  });
  resizeObserver.observe(scroller.value);
});
onBeforeUnmount(() => resizeObserver?.disconnect());
</script>
