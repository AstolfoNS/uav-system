<script setup lang="ts">
interface Props {
  modelValue: boolean;
  title: string;
  message: string;
  confirmText: string;
  cancelText: string;
  loading?: boolean;
}

withDefaults(defineProps<Props>(), {
  loading: false,
});

const emit = defineEmits<{
  (event: "update:modelValue", value: boolean): void;
  (event: "confirm"): void;
}>();

function close(): void {
  emit("update:modelValue", false);
}

function confirm(): void {
  emit("confirm");
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
      <v-card-text class="pt-4">{{ message }}</v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn :disabled="loading" variant="text" @click="close">{{
          cancelText
        }}</v-btn>
        <v-btn color="error" :loading="loading" @click="confirm">{{
          confirmText
        }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
