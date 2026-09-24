<template>
  <div class="flex flex-wrap gap-x-4 gap-y-1.5 text-xs text-muted">
    <span v-for="j in board.jobTypes" :key="j.keyword" class="inline-flex items-center">
      <i :class="[SWATCH, 'h-2.5']" :style="{ background: j.color }"></i>{{ j.displayName }}
    </span>
    <span class="inline-flex items-center"><WaitHatch :class="[SWATCH, 'h-2.5']" />ожидание окна</span>
    <span class="inline-flex items-center"><i :class="[SWATCH, 'h-0.5 bg-faint']"></i>в пути</span>
    <span class="inline-flex items-center"><i :class="[SWATCH, 'h-2.5 bg-line-strong']"></i>перерыв</span>
    <span class="inline-flex items-center">
      <i :class="[SWATCH, 'h-2.5 outline-2 -outline-offset-2 outline-urgent outline-dashed']"></i>срок сегодня
    </span>
  </div>
</template>

<script setup lang="ts">
import { useBoardStore } from "../stores/board";
import WaitHatch from "./WaitHatch.vue";

const SWATCH = "me-1.5 inline-block w-4.5 flex-none rounded-sm";

const board = useBoardStore();
</script>
