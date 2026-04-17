<script setup lang="ts">
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import { clearTokens } from "@/composables/useAuth";
import { notify } from "@/composables/useNotifier";
import { logout as logoutApi } from "@/api";
import { getApiBaseUrl } from "@/shared/http/request";
import AppSnackbar from "@/components/AppSnackbar.vue";
import { setLocale, type SupportedLocale } from "@/plugins/i18n";

const route = useRoute();
const router = useRouter();
const { t, locale } = useI18n();

const drawer = ref(true);
const busy = ref(false);
const languageMenu = ref(false);

const items = computed(() => [
  {
    title: t("app.menu.dashboard"),
    icon: "mdi-view-dashboard",
    to: "/dashboard",
  },
  {
    title: t("app.menu.profile"),
    icon: "mdi-account-edit",
    to: "/profile",
  },
  {
    title: t("app.menu.nodes"),
    icon: "mdi-access-point-network",
    to: "/nodes",
  },
  {
    title: t("app.menu.inference"),
    icon: "mdi-crosshairs-gps",
    to: "/inference",
  },
  { title: t("app.menu.records"), icon: "mdi-history", to: "/records" },
]);

const localeText = computed(() =>
  locale.value === "zh-CN" ? t("common.chinese") : t("common.english"),
);

const currentSection = computed(
  () =>
    items.value.find((item) => item.to === activePath.value)?.title ??
    t("app.title"),
);

function changeLocale(value: SupportedLocale): void {
  setLocale(value);
}

const activePath = computed(() => route.path);

async function logout(): Promise<void> {
  if (busy.value) {
    return;
  }

  busy.value = true;
  try {
    await logoutApi();
  } catch {
    // Ignore remote logout failures and clear local session anyway.
  } finally {
    clearTokens();
    busy.value = false;
    notify(t("app.signedOut"), "info");
    await router.push("/login");
  }
}
</script>

<template>
  <v-app>
    <v-navigation-drawer
      v-model="drawer"
      border="0"
      class="card-ambient app-sidenav"
      width="272"
    >
      <div class="sidenav-brand pa-4 d-flex align-center ga-3">
        <v-avatar class="brand-avatar" color="primary" variant="flat">
          <v-icon>mdi-helicopter</v-icon>
        </v-avatar>
        <div>
          <div class="text-subtitle-1 font-weight-bold text-white">
            {{ t("app.console") }}
          </div>
          <div class="text-caption brand-subtitle">
            {{ t("app.subtitle") }}
          </div>
        </div>
      </div>

      <div class="nav-section-label px-4 py-2 text-caption">Navigation</div>

      <v-list class="px-3" nav density="comfortable">
        <v-list-item
          v-for="item in items"
          :key="item.to"
          :active="activePath === item.to"
          active-class="nav-item--active"
          class="nav-item mb-1"
          :prepend-icon="item.icon"
          :title="item.title"
          rounded="lg"
          @click="router.push(item.to)"
        />
      </v-list>

      <template #append>
        <div class="pa-4 sidenav-footer">
          <v-chip
            class="mono mb-3 sidenav-url-chip"
            size="small"
            variant="outlined"
          >
            {{ getApiBaseUrl() }}
          </v-chip>
          <v-btn
            block
            color="error"
            prepend-icon="mdi-logout"
            :loading="busy"
            variant="tonal"
            @click="logout"
          >
            {{ t("app.signOut") }}
          </v-btn>
        </div>
      </template>
    </v-navigation-drawer>

    <v-app-bar class="app-topbar px-2" flat>
      <v-app-bar-nav-icon class="mr-1" @click="drawer = !drawer" />
      <div class="d-flex flex-column justify-center">
        <div class="text-caption topbar-overline">UAV SYSTEM</div>
        <v-toolbar-title class="page-title topbar-title">{{
          currentSection
        }}</v-toolbar-title>
      </div>
      <v-spacer />
      <v-menu v-model="languageMenu" location="bottom">
        <template #activator="{ props }">
          <v-btn
            class="mr-2 topbar-action btn-secondary-action"
            prepend-icon="mdi-translate"
            size="small"
            variant="tonal"
            v-bind="props"
          >
            {{ localeText }}
          </v-btn>
        </template>
        <v-list density="compact" nav>
          <v-list-item
            :active="locale === 'zh-CN'"
            :title="t('common.chinese')"
            @click="changeLocale('zh-CN')"
          />
          <v-list-item
            :active="locale === 'en-US'"
            :title="t('common.english')"
            @click="changeLocale('en-US')"
          />
        </v-list>
      </v-menu>
      <v-chip
        class="mono topbar-chip mr-2"
        color="primary"
        size="small"
        variant="flat"
      >
        {{ getApiBaseUrl() }}
      </v-chip>
    </v-app-bar>

    <v-main>
      <v-container class="page-container py-8">
        <RouterView />
      </v-container>
    </v-main>

    <AppSnackbar />
  </v-app>
</template>

<style scoped>
.page-container {
  max-width: 1520px;
  padding-inline: 10px;
}

.app-sidenav {
  background: linear-gradient(
    180deg,
    rgba(15, 112, 168, 0.96),
    rgba(35, 157, 224, 0.92)
  );
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.sidenav-brand {
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  background: linear-gradient(
    145deg,
    rgba(21, 127, 188, 0.78),
    rgba(36, 169, 240, 0.58)
  );
}

.brand-avatar {
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
}

.brand-subtitle {
  color: rgba(227, 242, 253, 0.78);
}

.nav-section-label {
  color: rgba(227, 242, 253, 0.66);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.nav-item {
  color: rgba(227, 242, 253, 0.9);
}

.nav-item :deep(.v-list-item__prepend > .v-icon) {
  opacity: 0.9;
}

.nav-item--active {
  background: linear-gradient(
    135deg,
    rgba(187, 222, 251, 0.25),
    rgba(255, 255, 255, 0.2)
  );
  color: #ffffff;
  border: 1px solid rgba(187, 222, 251, 0.34);
  box-shadow: 0 8px 16px rgba(6, 28, 56, 0.24);
}

.sidenav-footer {
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}

.sidenav-url-chip {
  width: 100%;
  justify-content: flex-start;
  color: rgba(238, 249, 255, 0.97);
  background: rgba(7, 39, 63, 0.52);
  border: 1px solid rgba(225, 242, 255, 0.34);
}

.sidenav-url-chip :deep(.v-chip__content) {
  color: rgba(238, 249, 255, 0.97);
}

.app-topbar {
  margin: 12px 28px 0 16px;
  border-radius: 16px;
  border: 1px solid rgba(var(--uav-primary-rgb), 0.25);
  background: linear-gradient(
    140deg,
    rgba(247, 253, 255, 0.97),
    rgba(234, 248, 255, 0.95)
  );
  box-shadow: 0 12px 24px rgba(var(--uav-primary-rgb), 0.14);
}

.topbar-overline {
  color: rgba(15, 108, 162, 0.74);
  letter-spacing: 0.08em;
  line-height: 1;
}

.topbar-title {
  font-size: 1.02rem;
  letter-spacing: 0.03em;
  text-transform: none;
  color: #157ebc;
  line-height: 1.2;
}

.topbar-action {
  border: 1px solid rgba(var(--uav-primary-rgb), 0.22);
}

.topbar-chip {
  border: 1px solid rgba(var(--uav-primary-rgb), 0.22);
  color: #0f4f77;
  background: rgba(216, 240, 255, 0.88);
}

@media (max-width: 960px) {
  .app-topbar {
    margin: 8px 8px 0;
    border-radius: 12px;
  }
}
</style>
