<template>
  <div class="flex flex-wrap gap-1.5" role="group" aria-label="Тип работ">
    <UiButton :pressed="board.jobType === ''" @click="board.jobType = ''">Все типы работ</UiButton>
    <UiButton
      v-for="j in board.jobTypes"
      :key="j.keyword"
      :pressed="board.jobType === j.keyword"
      @click="board.jobType = j.keyword"
    >
      <UiDot :color="j.color" />{{ j.displayName }}
    </UiButton>
  </div>
</template>

<script setup lang="ts">
import UiButton from "@/components/ui/UiButton.vue";
import UiDot from "@/components/ui/UiDot.vue";
import { useBoardStore } from "../stores/board";

const board = useBoardStore();
</script>
