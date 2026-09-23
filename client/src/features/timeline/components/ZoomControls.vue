<template>
  <div class="flex items-center gap-1.5" role="group" aria-label="Масштаб шкалы времени">
    <UiButton size="icon" :disabled="board.zoom <= ZOOM_MIN" aria-label="Уменьшить масштаб" @click="board.zoomOut()">
      −
    </UiButton>
    <UiButton
      class="mono min-w-[58px] justify-center"
      title="Сбросить масштаб · Ctrl + колесо мыши для масштабирования"
      @click="board.setZoom(1)"
    >
      {{ Math.round(board.zoom * 100) }}%
    </UiButton>
    <UiButton size="icon" :disabled="board.zoom >= ZOOM_MAX" aria-label="Увеличить масштаб" @click="board.zoomIn()">
      +
    </UiButton>
  </div>
</template>

<script setup lang="ts">
import UiButton from "@/components/ui/UiButton.vue";
import { useBoardStore, ZOOM_MAX, ZOOM_MIN } from "../stores/board";

const board = useBoardStore();
</script>
