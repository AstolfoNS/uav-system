<script setup lang="ts">
defineProps<{
  title: string;
  subtitle: string;
}>();
</script>

<template>
  <v-card class="page-hero card-spacious page-hero-shell" rounded="xl">
    <v-card-title class="page-hero-head d-flex flex-wrap align-center ga-3">
      <div class="page-hero-copy">
        <div class="text-h5 font-weight-bold page-hero-title">
          {{ title }}
        </div>
        <div class="text-body-2 page-hero-subtitle mt-1">
          {{ subtitle }}
        </div>
      </div>
      <v-spacer />
      <div class="page-hero-actions d-flex flex-wrap align-center ga-2">
        <slot name="actions" />
      </div>
    </v-card-title>
    <v-card-text v-if="$slots.default" class="page-hero-extra">
      <slot />
    </v-card-text>
  </v-card>
</template>

<style scoped>
.page-hero-shell {
  overflow: hidden;
}

.page-hero-head {
  min-height: 92px;
}

.page-hero-copy {
  max-width: min(720px, 100%);
}

.page-hero-actions {
  justify-content: flex-end;
}

.page-hero-extra {
  padding-top: 0;
}

@media (max-width: 960px) {
  .page-hero-head {
    min-height: 80px;
  }

  .page-hero-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
