<template>
  <!-- Native modal dialog: the browser keeps focus inside, closes it on Escape and renders the backdrop -->
  <dialog
    ref="dialogEl"
    class="m-auto w-[min(520px,calc(100vw-32px))] max-h-[calc(100dvh-32px)] overflow-y-auto overscroll-contain rounded-lg border border-line bg-surface p-0 text-ink shadow-lg backdrop:bg-black/50"
    :aria-labelledby="titleId"
    @cancel="onCancel"
    @close="open = false"
    @click="onClick"
  >
    <div class="grid gap-4 p-5">
      <header class="flex items-start justify-between gap-4">
        <h2 :id="titleId" class="m-0 text-lg font-semibold text-balance text-ink">{{ title }}</h2>
        <button
          type="button"
          class="-me-1.5 -mt-1 inline-flex size-8 shrink-0 cursor-pointer items-center justify-center rounded-lg text-muted hover:bg-shift hover:text-ink focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50 disabled:pointer-events-none disabled:opacity-50"
          aria-label="Закрыть"
          :disabled="!dismissible"
          @click="open = false"
        >
          <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
            <path d="M18 6 6 18M6 6l12 12" />
          </svg>
        </button>
      </header>

      <slot />
    </div>
  </dialog>
</template>

<script lang="ts">
// Page scroll lock, shared by every open dialog: the page behind a modal stays put, and it is only
// released when the last one closes
let openDialogCount = 0;
let savedOverflow = "";
let savedPaddingRight = "";

function lockPageScroll(): void {
  if (openDialogCount++ > 0) return;
  const root = document.documentElement;
  // the scrollbar disappears with the overflow: keep its width so the page doesn't shift sideways
  const scrollbarWidth = window.innerWidth - root.clientWidth;
  savedOverflow = root.style.overflow;
  savedPaddingRight = root.style.paddingRight;
  root.style.overflow = "hidden";
  if (scrollbarWidth > 0) root.style.paddingRight = `${scrollbarWidth}px`;
}

function unlockPageScroll(): void {
  if (openDialogCount === 0 || --openDialogCount > 0) return;
  const root = document.documentElement;
  root.style.overflow = savedOverflow;
  root.style.paddingRight = savedPaddingRight;
}
</script>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, useId, watch } from "vue";

const props = withDefaults(
  defineProps<{
    title: string;
    /** false while something is in flight: Escape, the backdrop and the close button do nothing */
    dismissible?: boolean;
  }>(),
  { dismissible: true },
);

const open = defineModel<boolean>("open", { required: true });

const titleId = useId();

const dialogEl = ref<HTMLDialogElement | null>(null);

/** this dialog holds the page scroll lock */
let holdsLock = false;

function sync(isOpen: boolean): void {
  const dialog = dialogEl.value;
  if (!dialog) return;
  if (isOpen && !dialog.open) dialog.showModal();
  else if (!isOpen && dialog.open) dialog.close();

  if (isOpen && !holdsLock) lockPageScroll();
  else if (!isOpen && holdsLock) unlockPageScroll();
  holdsLock = isOpen;
}

watch(open, sync);
onMounted(() => sync(open.value));
// unmounted while open (e.g. the page is left): release the lock
onBeforeUnmount(() => {
  if (holdsLock) unlockPageScroll();
  holdsLock = false;
});

function onCancel(event: Event): void {
  if (!props.dismissible) event.preventDefault();
}

/** a click on the dialog element itself (not its content) is a click on the backdrop */
function onClick(event: MouseEvent): void {
  if (event.target === dialogEl.value && props.dismissible) open.value = false;
}
</script>
