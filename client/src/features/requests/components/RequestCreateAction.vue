<template>
  <UiButton variant="solid" @click="emit('open')">
    <svg
      class="size-4"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      aria-hidden="true"
    >
      <path d="M12 5v14M5 12h14" />
    </svg>
    <!-- the "+" alone on narrow screens, where the header also holds the section tabs -->
    <span class="max-sm:sr-only">Создать заявку</span>
  </UiButton>

  <UiDialog v-model:open="dialogOpen" title="Новая заявка" :dismissible="!pending">
    <form class="grid gap-4" novalidate @submit.prevent="submit">
      <div class="grid gap-1.5">
        <label :for="`${formId}-region`" class="text-sm font-medium text-ink">
          Регион<span class="ms-0.5 text-danger" aria-hidden="true">*</span>
        </label>
        <div class="relative">
          <select
            :id="`${formId}-region`"
            v-model="form.regionId"
            :class="[FIELD_CLASS, 'cursor-pointer appearance-none pe-9']"
            aria-required="true"
            :aria-invalid="!!shownErrors.regionId"
          >
            <option :value="null" disabled>Выберите регион</option>
            <option v-for="region in regions" :key="region.id" :value="region.id">{{ region.displayName }}</option>
          </select>
          <svg
            class="pointer-events-none absolute end-3 top-1/2 size-3.5 -translate-y-1/2 text-muted"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="m7 15 5 5 5-5M7 9l5-5 5 5" />
          </svg>
        </div>
        <p v-if="shownErrors.regionId" class="m-0 text-xs text-danger">{{ shownErrors.regionId }}</p>
      </div>

      <div class="grid gap-1.5">
        <label :for="`${formId}-address`" class="text-sm font-medium text-ink">
          Адрес<span class="ms-0.5 text-danger" aria-hidden="true">*</span>
        </label>
        <input
          :id="`${formId}-address`"
          v-model="form.address"
          type="text"
          :class="FIELD_CLASS"
          placeholder="Город, улица, дом"
          autocomplete="street-address"
          aria-required="true"
          :aria-invalid="!!shownErrors.address"
        />
        <p v-if="shownErrors.address" class="m-0 text-xs text-danger">{{ shownErrors.address }}</p>
      </div>

      <div class="grid gap-4 sm:grid-cols-2">
        <div class="grid content-start gap-1.5">
          <label :for="`${formId}-job-type`" class="text-sm font-medium text-ink">
            Тип работ<span class="ms-0.5 text-danger" aria-hidden="true">*</span>
          </label>
          <div class="relative">
            <select
              :id="`${formId}-job-type`"
              v-model="form.jobTypeId"
              :class="[FIELD_CLASS, 'cursor-pointer appearance-none pe-9']"
              aria-required="true"
              :aria-invalid="!!shownErrors.jobTypeId"
            >
              <option :value="null" disabled>Выберите тип</option>
              <option v-for="jobType in dictionaries.jobTypes" :key="jobType.id" :value="jobType.id">
                {{ jobType.displayName }}
              </option>
            </select>
            <svg
              class="pointer-events-none absolute end-3 top-1/2 size-3.5 -translate-y-1/2 text-muted"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="m7 15 5 5 5-5M7 9l5-5 5 5" />
            </svg>
          </div>
          <p v-if="shownErrors.jobTypeId" class="m-0 text-xs text-danger">{{ shownErrors.jobTypeId }}</p>
          <p v-else-if="isEmergency" class="m-0 text-xs text-urgent">
            Срочная: расписание на сегодня будет пересчитано
          </p>
        </div>

        <div class="grid content-start gap-1.5">
          <label :for="`${formId}-vehicle`" class="text-sm font-medium text-ink">Транспорт</label>
          <div class="relative">
            <select
              :id="`${formId}-vehicle`"
              v-model="form.vehicleId"
              :class="[FIELD_CLASS, 'cursor-pointer appearance-none pe-9']"
            >
              <option :value="null">Любой</option>
              <option v-for="vehicle in dictionaries.vehicles" :key="vehicle.id" :value="vehicle.id">
                {{ vehicle.displayName }}
              </option>
            </select>
            <svg
              class="pointer-events-none absolute end-3 top-1/2 size-3.5 -translate-y-1/2 text-muted"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="m7 15 5 5 5-5M7 9l5-5 5 5" />
            </svg>
          </div>
        </div>
      </div>

      <!-- the equipment the crew must bring, from the server's list -->
      <div class="grid gap-1.5">
        <label :for="`${formId}-equipment`" class="text-sm font-medium text-ink">Оборудование</label>
        <div class="relative">
          <select
            :id="`${formId}-equipment`"
            v-model="form.equipmentId"
            :class="[FIELD_CLASS, 'cursor-pointer appearance-none pe-9']"
          >
            <option :value="null">Не требуется</option>
            <option v-for="item in dictionaries.equipment" :key="item.id" :value="item.id">{{ item.displayName }}</option>
          </select>
          <svg
            class="pointer-events-none absolute end-3 top-1/2 size-3.5 -translate-y-1/2 text-muted"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="m7 15 5 5 5-5M7 9l5-5 5 5" />
          </svg>
        </div>
      </div>

      <!-- an emergency is handled as soon as possible: it takes no time window (disabling the fieldset
           disables both inputs) -->
      <fieldset class="m-0 grid gap-1.5 border-0 p-0" :disabled="isEmergency">
        <legend class="mb-1.5 p-0 text-sm font-medium" :class="isEmergency ? 'text-muted' : 'text-ink'">
          Временное окно
        </legend>
        <div class="flex flex-wrap items-center gap-2">
          <input
            v-model="form.windowStart"
            type="time"
            :class="[FIELD_CLASS, 'mono w-32! disabled:cursor-not-allowed disabled:opacity-50']"
            aria-label="Начало окна"
            :aria-invalid="!!shownErrors.window"
          />
          <span class="text-muted">–</span>
          <input
            v-model="form.windowEnd"
            type="time"
            :class="[FIELD_CLASS, 'mono w-32! disabled:cursor-not-allowed disabled:opacity-50']"
            aria-label="Конец окна"
            :aria-invalid="!!shownErrors.window"
          />
        </div>
        <p v-if="isEmergency" class="m-0 text-xs text-muted">Аварийная заявка выполняется без временного окна</p>
        <p v-else-if="shownErrors.window" class="m-0 text-xs text-danger">{{ shownErrors.window }}</p>
      </fieldset>

      <p v-if="error" class="m-0 rounded-md bg-danger/10 px-3 py-2 text-sm text-danger" role="alert">{{ error }}</p>

      <div class="flex flex-wrap justify-end gap-2 border-t border-line pt-4">
        <UiButton :disabled="pending" @click="dialogOpen = false">Отмена</UiButton>
        <UiButton variant="solid" type="submit" :disabled="pending">
          <UiDot v-if="pending" size="sm" color="currentColor" pulse />
          {{ pending ? "Создаём…" : "Создать" }}
        </UiButton>
      </div>
    </form>
  </UiDialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, useId, watch } from "vue";
import UiButton from "@/components/ui/UiButton.vue";
import UiDialog from "@/components/ui/UiDialog.vue";
import UiDot from "@/components/ui/UiDot.vue";
import { useDictionariesStore } from "@/features/dictionaries/stores/dictionaries";
import type { RegionInfo, RequestCreateBody } from "../types";

// the job types, vehicles and equipment as the server has them (loaded when the dialog opens)
const dictionaries = useDictionariesStore();

const props = defineProps<{
  regions: RegionInfo[];
  /** the region the page shows; preselected in the form */
  regionId: number | null;
  /** the dialog is open */
  open: boolean;
  /** the request is being created */
  pending: boolean;
  /** why the last attempt failed */
  error: string | null;
}>();

const emit = defineEmits<{
  open: [];
  close: [];
  submit: [body: RequestCreateBody];
}>();

/** the field look of UiInput / UiSelect, stretched to the form's width; red border when invalid */
const FIELD_CLASS =
  "h-9.5 w-full min-w-0 rounded-lg border border-line bg-surface px-3 py-2 text-sm text-ink shadow-2xs placeholder:text-faint focus:border-accent focus:ring-2 focus:ring-accent/30 focus:outline-hidden aria-invalid:border-danger";

const formId = useId();

const dialogOpen = computed({
  get: () => props.open,
  set: (isOpen) => emit(isOpen ? "open" : "close"),
});

interface FormState {
  regionId: number | null;
  address: string;
  jobTypeId: number | null;
  vehicleId: number | null;
  equipmentId: number | null;
  /** "HH:mm" from the time inputs, "" when empty */
  windowStart: string;
  windowEnd: string;
}

const emptyForm = (): FormState => ({
  regionId: props.regionId,
  address: "",
  jobTypeId: null,
  vehicleId: null,
  equipmentId: null,
  windowStart: "",
  windowEnd: "",
});

const form = reactive<FormState>(emptyForm());
/** errors show only after the first submit attempt, then follow the input */
const submitted = ref(false);

// every opening starts with a clean form in the page's current region
watch(
  () => props.open,
  (isOpen) => {
    if (!isOpen) return;
    Object.assign(form, emptyForm());
    submitted.value = false;
  },
);

const isEmergency = computed(
  () => dictionaries.jobTypes.find((jobType) => jobType.id === form.jobTypeId)?.keyword === "EMERGENCY",
);

// an emergency takes no time window: times entered before switching to it are dropped, not just hidden
watch(isEmergency, (emergency) => {
  if (!emergency) return;
  form.windowStart = "";
  form.windowEnd = "";
});

const errors = computed(() => {
  const found: Partial<Record<"regionId" | "address" | "jobTypeId" | "window", string>> = {};
  if (form.regionId == null) found.regionId = "Выберите регион";
  if (!form.address.trim()) found.address = "Укажите адрес";
  if (form.jobTypeId == null) found.jobTypeId = "Выберите тип работ";
  if (!isEmergency.value && !!form.windowStart !== !!form.windowEnd) {
    found.window = "Укажите обе границы окна или оставьте обе пустыми";
  }
  return found;
});

const shownErrors = computed(() => (submitted.value ? errors.value : {}));

/** "HH:mm" from a time input → "HH:mm:ss" for the server's LocalTime */
const toServerTime = (time: string): string | null => (time ? `${time.slice(0, 5)}:00` : null);

function submit(): void {
  submitted.value = true;
  if (props.pending || Object.keys(errors.value).length) return;
  emit("submit", {
    regionId: form.regionId as number,
    address: form.address.trim(),
    jobTypeId: form.jobTypeId as number,
    requiredVehicleId: form.vehicleId,
    requiredEquipmentId: form.equipmentId,
    // an emergency is sent without a window; the server then takes the whole day
    startTimeWindow: isEmergency.value ? null : toServerTime(form.windowStart),
    endTimeWindow: isEmergency.value ? null : toServerTime(form.windowEnd),
  });
}
</script>
