<template>
  <div class="grid justify-items-end gap-2">
    <!-- canceling can't be undone here, so it asks once more -->
    <div
      v-if="confirming"
      class="flex flex-wrap items-center justify-end gap-2 rounded-lg border border-danger/40 bg-danger/10 p-2 ps-3"
      role="alertdialog"
      :aria-label="`Отменить заявку №${requestId}?`"
    >
      <span class="text-sm font-medium text-danger">Отменить заявку №{{ requestId }}?</span>
      <button
        type="button"
        class="inline-flex h-9.5 cursor-pointer items-center justify-center gap-x-2 rounded-lg bg-danger px-3 text-sm font-medium text-white hover:brightness-110 focus:outline-hidden focus-visible:ring-2 focus-visible:ring-danger/50"
        @click="confirm"
      >
        Да, отменить
      </button>
      <UiButton @click="confirming = false">Нет</UiButton>
    </div>

    <UiButton v-else class="text-danger!" :disabled="canceling" @click="confirming = true">
      <UiDot v-if="canceling" size="sm" pulse />
      <svg
        v-else
        class="size-4"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <circle cx="12" cy="12" r="10" />
        <path d="m15 9-6 6M9 9l6 6" />
      </svg>
      {{ canceling ? "Отменяем…" : "Отменить заявку" }}
    </UiButton>

    <p v-if="error" class="m-0 max-w-sm rounded-md bg-danger/10 px-3 py-1.5 text-xs text-danger" role="alert">
      {{ error }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import UiButton from "@/components/ui/UiButton.vue";
import UiDot from "@/components/ui/UiDot.vue";

defineProps<{
  requestId: number;
  /** the cancel request is in flight */
  canceling: boolean;
  /** why the last cancel failed */
  error: string | null;
}>();

const emit = defineEmits<{
  cancel: [];
}>();

const confirming = ref(false);

function confirm(): void {
  confirming.value = false;
  emit("cancel");
}
</script>
