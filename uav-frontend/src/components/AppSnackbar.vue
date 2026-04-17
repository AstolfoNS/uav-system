<script setup lang="ts">
import { notifierState } from "@/composables/useNotifier";
</script>

<template>
  <v-snackbar
    v-model="notifierState.show"
    :color="notifierState.color"
    :timeout="notifierState.timeout"
    location="top right"
    variant="flat"
  >
    {{ notifierState.text }}
  </v-snackbar>
</template>
