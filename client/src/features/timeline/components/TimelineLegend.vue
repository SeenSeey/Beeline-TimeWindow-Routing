<template>
  <div class="flex flex-wrap gap-x-4 gap-y-1.5 text-[12px] text-muted">
    <span v-for="j in board.jobTypes" :key="j.keyword" class="inline-flex items-center">
      <i :class="[SWATCH, 'h-[10px]']" :style="{ background: j.color }"></i>{{ j.displayName }}
    </span>
    <span class="inline-flex items-center"><WaitHatch :class="[SWATCH, 'h-[10px]']" />ожидание окна</span>
    <span class="inline-flex items-center"><i :class="[SWATCH, 'h-0.5 bg-faint']"></i>в пути</span>
    <span class="inline-flex items-center"><i :class="[SWATCH, 'h-[10px] bg-line-strong']"></i>перерыв</span>
    <span class="inline-flex items-center">
      <i :class="[SWATCH, 'h-[10px] outline-2 -outline-offset-2 outline-urgent outline-dashed']"></i>срок сегодня
    </span>
  </div>
</template>

<script setup lang="ts">
import { useBoardStore } from "../stores/board";
import WaitHatch from "./WaitHatch.vue";

const SWATCH = "mr-[5px] inline-block w-[18px] flex-none rounded-[2px]";

const board = useBoardStore();
</script>
