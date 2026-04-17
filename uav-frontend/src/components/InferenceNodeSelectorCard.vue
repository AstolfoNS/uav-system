<script setup lang="ts">
import { useI18n } from "vue-i18n";
import type { YoloNode } from "@/types";

defineProps<{
  nodes: YoloNode[];
  modelValue: number | null;
}>();

const emit = defineEmits<{
  (event: "update:modelValue", value: number | null): void;
}>();

const { t } = useI18n();
</script>

<template>
  <v-card class="node-side-card" rounded="xl" variant="elevated">
    <v-card-title class="text-subtitle-2 font-weight-bold">
      <slot name="title">{{ t("inference.targetNode") }}</slot>
    </v-card-title>
    <v-card-text>
      <v-select
        :model-value="modelValue"
        :items="nodes"
        item-title="nodeName"
        item-value="id"
        :label="t('inference.targetNode')"
        prepend-inner-icon="mdi-access-point-network"
        variant="outlined"
        @update:model-value="emit('update:modelValue', $event as number | null)"
      />
    </v-card-text>
  </v-card>
</template>

<style scoped>
.node-side-card {
  border: 1px solid rgba(var(--uav-primary-rgb), 0.2);
  box-shadow: 0 10px 20px rgba(var(--uav-primary-rgb), 0.1);
}

.node-side-card :deep(.v-card-title) {
  border-bottom: 1px solid rgba(var(--uav-primary-rgb), 0.14);
}
</style>
