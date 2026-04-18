<script setup lang="ts">
import { computed, ref, watch } from "vue";

interface Props {
  modelValue: boolean;
  title: string;
  label: string;
  confirmText: string;
  cancelText: string;
  loading?: boolean;
  initialValue?: string;
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  initialValue: "",
});

const emit = defineEmits<{
  (event: "update:modelValue", value: boolean): void;
  (event: "confirm", value: string): void;
}>();

const value = ref("");

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      value.value = props.initialValue;
    }
  },
);

const disabled = computed(() => !value.value.trim() || props.loading);

function close(): void {
  emit("update:modelValue", false);
}

function confirm(): void {
  const next = value.value.trim();
  if (!next) {
    return;
  }
  emit("confirm", next);
}
</script>

<template>
  <v-dialog
    :model-value="modelValue"
    max-width="520"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <v-card rounded="xl">
      <v-card-title>{{ title }}</v-card-title>
      <v-divider />
      <v-card-text class="pt-4">
        <v-text-field
          v-model="value"
          :label="label"
          autofocus
          variant="outlined"
          @keyup.enter="confirm"
        />
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn :disabled="loading" variant="text" @click="close">{{
          cancelText
        }}</v-btn>
        <v-btn
          color="primary"
          :disabled="disabled"
          :loading="loading"
          @click="confirm"
          >{{ confirmText }}</v-btn
        >
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
