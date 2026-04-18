<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import { hasAccessToken } from "@/composables/useAuth";

const router = useRouter();
const { t } = useI18n();

const actionText = computed(() =>
  hasAccessToken() ? t("app.notFoundBackHome") : t("app.notFoundBackLogin"),
);

async function back(): Promise<void> {
  await router.push(hasAccessToken() ? "/dashboard" : "/login");
}
</script>

<template>
  <v-row align="center" class="fill-height" justify="center">
    <v-col cols="12" md="8" lg="6">
      <v-card class="card-ambient text-center" rounded="xl">
        <v-card-text class="py-12">
          <div class="text-h1 font-weight-black">404</div>
          <div class="text-h5 mt-2">{{ t("app.notFoundTitle") }}</div>
          <div class="text-body-1 text-medium-emphasis mt-2">
            {{ t("app.notFoundDesc") }}
          </div>
          <v-btn
            class="mt-6"
            color="primary"
            prepend-icon="mdi-arrow-left"
            @click="back"
          >
            {{ actionText }}
          </v-btn>
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>
</template>
