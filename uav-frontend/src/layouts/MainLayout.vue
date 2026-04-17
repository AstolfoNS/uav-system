<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import { clearTokens } from "@/composables/useAuth";
import { notify } from "@/composables/useNotifier";
import {
  fetchMyProfile,
  fetchNodePage,
  fetchRecordPage,
  logout as logoutApi,
} from "@/api";
import { getApiBaseUrl } from "@/shared/http/request";
import AppSnackbar from "@/components/AppSnackbar.vue";
import { setLocale, type SupportedLocale } from "@/plugins/i18n";

type NavItem = {
  title: string;
  icon: string;
  to: string;
  badge?: string;
  badgeType?: "info" | "success";
};

type NavGroup = {
  key: string;
  title: string;
  icon: string;
  items: NavItem[];
};

const route = useRoute();
const router = useRouter();
const { t, locale } = useI18n();

const drawer = ref(true);
const busy = ref(false);
const languageMenu = ref(false);
const profileMenu = ref(false);
const profileBusy = ref(false);
const topbarAvatarUrl = ref("/images/default-avatar.svg");
const profileRoles = ref<string[]>([]);
const selectedGroupKey = ref("overview");
const recordsTotal = ref<number | null>(null);
const onlineNodesTotal = ref<number | null>(null);
const statsBusy = ref(false);
let statsTimer: ReturnType<typeof setInterval> | null = null;

const STATS_REFRESH_MS = 45_000;

const isAdmin = computed(() =>
  profileRoles.value.some((role) => role.toLowerCase() === "admin"),
);

const navGroups = computed<NavGroup[]>(() => [
  {
    key: "overview",
    title: "Overview",
    icon: "mdi-view-dashboard-outline",
    items: [
      {
        title: t("app.menu.dashboard"),
        icon: "mdi-view-dashboard",
        to: "/dashboard",
      },
    ],
  },
  {
    key: "ops",
    title: "Operations",
    icon: "mdi-radar",
    items: [
      {
        title: t("app.menu.inference"),
        icon: "mdi-crosshairs-gps",
        to: "/inference",
      },
      {
        title: t("app.menu.records"),
        icon: "mdi-history",
        to: "/records",
        badge:
          recordsTotal.value === null ? undefined : String(recordsTotal.value),
        badgeType: "info",
      },
    ],
  },
  {
    key: "infra",
    title: "Infrastructure",
    icon: "mdi-server-network",
    items: [
      {
        title: t("app.menu.nodes"),
        icon: "mdi-access-point-network",
        to: "/nodes",
        badge:
          onlineNodesTotal.value === null
            ? undefined
            : `${onlineNodesTotal.value} ${t("node.online")}`,
        badgeType: "success",
      },
    ],
  },
  {
    key: "account",
    title: "Account",
    icon: "mdi-account-circle-outline",
    items: [
      {
        title: t("app.menu.profile"),
        icon: "mdi-account-circle-outline",
        to: "/profile",
      },
    ],
  },
  ...(isAdmin.value
    ? [
        {
          key: "security",
          title: "Security",
          icon: "mdi-shield-account-outline",
          items: [
            {
              title: t("app.menu.rbac"),
              icon: "mdi-account-key-outline",
              to: "/rbac",
            },
          ],
        } satisfies NavGroup,
      ]
    : []),
]);

const selectedGroup = computed<NavGroup | undefined>(() =>
  navGroups.value.find((group) => group.key === selectedGroupKey.value),
);

const visibleItems = computed<NavItem[]>(
  () => selectedGroup.value?.items ?? [],
);

const localeText = computed(() =>
  locale.value === "zh-CN" ? t("common.chinese") : t("common.english"),
);

const currentSection = computed(() => {
  const titleMap: Record<string, string> = {
    "/dashboard": t("app.menu.dashboard"),
    "/profile": t("app.menu.profile"),
    "/nodes": t("app.menu.nodes"),
    "/inference": t("app.menu.inference"),
    "/records": t("app.menu.records"),
    "/rbac": t("app.menu.rbac"),
  };

  return titleMap[activePath.value] ?? t("app.title");
});

function changeLocale(value: SupportedLocale): void {
  setLocale(value);
}

const activePath = computed(() => route.path);

function syncSelectedGroupByPath(path: string): void {
  const matchedGroup = navGroups.value.find((group) =>
    group.items.some((item) => item.to === path),
  );
  if (matchedGroup) {
    selectedGroupKey.value = matchedGroup.key;
  }
}

function selectGroup(groupKey: string): void {
  selectedGroupKey.value = groupKey;
  const group = navGroups.value.find((item) => item.key === groupKey);
  if (!group || group.items.length === 0) {
    return;
  }

  const hasActiveItem = group.items.some(
    (item) => item.to === activePath.value,
  );
  if (!hasActiveItem) {
    void router.push(group.items[0].to);
  }
}

async function loadTopbarProfile(): Promise<void> {
  if (profileBusy.value) {
    return;
  }

  profileBusy.value = true;
  try {
    const profile = await fetchMyProfile();
    const avatarUrl = String(profile.avatarUrl ?? "").trim();
    topbarAvatarUrl.value = avatarUrl || "/images/default-avatar.svg";
    profileRoles.value = Array.isArray(profile.roles)
      ? profile.roles.map((item) => String(item))
      : [];
  } catch {
    topbarAvatarUrl.value = "/images/default-avatar.svg";
    profileRoles.value = [];
  } finally {
    profileBusy.value = false;
  }
}

async function loadMenuStats(): Promise<void> {
  if (statsBusy.value) {
    return;
  }

  statsBusy.value = true;
  try {
    const [recordPage, onlineNodePage] = await Promise.all([
      fetchRecordPage({ current: 1, size: 1 }),
      fetchNodePage({ current: 1, size: 1, status: 1 }),
    ]);

    recordsTotal.value = Number(recordPage.total ?? 0);
    onlineNodesTotal.value = Number(onlineNodePage.total ?? 0);
  } catch {
    // Keep previous stats to avoid visual flicker when transient errors occur.
  } finally {
    statsBusy.value = false;
  }
}

function startStatsPolling(): void {
  if (statsTimer) {
    clearInterval(statsTimer);
  }
  statsTimer = setInterval(() => {
    void loadMenuStats();
  }, STATS_REFRESH_MS);
}

function stopStatsPolling(): void {
  if (!statsTimer) {
    return;
  }
  clearInterval(statsTimer);
  statsTimer = null;
}

function goProfile(): void {
  profileMenu.value = false;
  void router.push("/profile");
}

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

onMounted(() => {
  syncSelectedGroupByPath(route.path);
  void loadTopbarProfile();
  void loadMenuStats();
  startStatsPolling();
});

onBeforeUnmount(() => {
  stopStatsPolling();
});

watch(
  () => route.fullPath,
  () => {
    syncSelectedGroupByPath(route.path);
    void loadTopbarProfile();
    void loadMenuStats();
  },
);
</script>

<template>
  <v-app>
    <v-navigation-drawer
      v-model="drawer"
      border="0"
      class="card-ambient app-sidenav"
      :width="drawer ? 368 : 92"
    >
      <div class="sidenav-root">
        <div class="rail-column">
          <div class="rail-brand pa-3 d-flex justify-center">
            <v-avatar class="brand-avatar" color="primary" variant="flat">
              <v-icon>mdi-helicopter</v-icon>
            </v-avatar>
          </div>

          <div class="rail-menu py-2">
            <v-tooltip
              v-for="group in navGroups"
              :key="group.key"
              location="right"
              :text="group.title"
            >
              <template #activator="{ props }">
                <v-btn
                  class="rail-btn"
                  :class="{
                    'rail-btn--active': selectedGroupKey === group.key,
                  }"
                  icon
                  size="44"
                  variant="text"
                  v-bind="props"
                  @click="selectGroup(group.key)"
                >
                  <v-icon>{{ group.icon }}</v-icon>
                </v-btn>
              </template>
            </v-tooltip>
          </div>

          <div class="rail-footer pa-3">
            <v-btn
              block
              class="rail-logout"
              color="error"
              icon="mdi-logout"
              :loading="busy"
              variant="tonal"
              @click="logout"
            />
          </div>
        </div>

        <div v-show="drawer" class="menu-column">
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

          <div class="menu-group-head px-4 py-3">
            <div class="text-overline menu-group-caption">Section</div>
            <div class="text-subtitle-2 font-weight-bold text-white">
              {{ selectedGroup?.title ?? "Navigation" }}
            </div>
          </div>

          <v-list class="px-3" nav density="comfortable">
            <v-list-item
              v-for="item in visibleItems"
              :key="item.to"
              :active="activePath === item.to"
              active-class="nav-item--active"
              class="nav-item mb-1"
              :prepend-icon="item.icon"
              :title="item.title"
              rounded="lg"
              @click="router.push(item.to)"
            >
              <template #append>
                <v-chip
                  v-if="item.badge"
                  class="menu-stat-badge"
                  :class="{
                    'menu-stat-badge--success': item.badgeType === 'success',
                    'menu-stat-badge--info': item.badgeType !== 'success',
                  }"
                  size="x-small"
                  variant="flat"
                >
                  {{ item.badge }}
                </v-chip>
              </template>
            </v-list-item>
          </v-list>

          <div class="pa-4 sidenav-footer mt-auto">
            <v-chip
              class="mono mb-3 sidenav-url-chip"
              size="small"
              variant="outlined"
            >
              {{ getApiBaseUrl() }}
            </v-chip>
          </div>
        </div>
      </div>
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
      <div class="d-flex align-center ga-2 topbar-controls">
        <v-menu v-model="languageMenu" location="bottom">
          <template #activator="{ props }">
            <v-btn
              class="topbar-action btn-secondary-action"
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
          class="mono topbar-chip"
          color="primary"
          size="small"
          variant="flat"
        >
          {{ getApiBaseUrl() }}
        </v-chip>
        <v-menu v-model="profileMenu" location="bottom end">
          <template #activator="{ props }">
            <v-btn class="topbar-avatar-btn" icon v-bind="props">
              <v-avatar size="36">
                <v-img :src="topbarAvatarUrl" cover />
              </v-avatar>
            </v-btn>
          </template>
          <v-list density="compact" nav>
            <v-list-item
              prepend-icon="mdi-account-circle-outline"
              :title="t('app.menu.profile')"
              @click="goProfile"
            />
            <v-list-item
              prepend-icon="mdi-logout"
              :title="t('app.signOut')"
              @click="logout"
            />
          </v-list>
        </v-menu>
      </div>
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
  max-width: 1560px;
  padding-inline: 14px;
}

.app-sidenav {
  background: linear-gradient(
    180deg,
    rgba(26, 64, 80, 0.96),
    rgba(36, 92, 112, 0.94)
  );
  border-right: 1px solid rgba(210, 233, 240, 0.2);
  box-shadow: 0 18px 34px rgba(13, 30, 40, 0.28);
  overflow: hidden;
}

.sidenav-root {
  display: flex;
  height: 100%;
}

.rail-column {
  width: 92px;
  border-right: 1px solid rgba(214, 238, 245, 0.16);
  background: linear-gradient(
    180deg,
    rgba(15, 43, 56, 0.36),
    rgba(12, 34, 44, 0.28)
  );
  display: flex;
  flex-direction: column;
}

.rail-brand {
  border-bottom: 1px solid rgba(214, 238, 245, 0.14);
}

.rail-menu {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.rail-btn {
  color: rgba(230, 245, 249, 0.84);
  border: 1px solid transparent;
}

.rail-btn--active {
  color: #ffffff;
  border-color: rgba(216, 241, 246, 0.4);
  background: linear-gradient(
    145deg,
    rgba(174, 220, 232, 0.34),
    rgba(248, 253, 255, 0.24)
  );
  box-shadow: 0 10px 20px rgba(8, 22, 30, 0.28);
}

.rail-footer {
  margin-top: auto;
  border-top: 1px solid rgba(214, 238, 245, 0.14);
}

.rail-logout {
  min-width: 0;
}

.menu-column {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.sidenav-brand {
  border-bottom: 1px solid rgba(216, 238, 244, 0.2);
  background: linear-gradient(
    145deg,
    rgba(35, 105, 127, 0.8),
    rgba(64, 136, 158, 0.62)
  );
}

.brand-avatar {
  box-shadow: 0 10px 22px rgba(6, 20, 28, 0.34);
}

.brand-subtitle {
  color: rgba(222, 239, 245, 0.8);
}

.menu-group-head {
  border-bottom: 1px solid rgba(214, 238, 245, 0.14);
  background: linear-gradient(
    145deg,
    rgba(26, 74, 92, 0.4),
    rgba(42, 102, 123, 0.25)
  );
}

.menu-group-caption {
  color: rgba(213, 236, 242, 0.7);
  letter-spacing: 0.08em;
}

.nav-item {
  color: rgba(231, 244, 248, 0.94);
}

.nav-item :deep(.v-list-item__prepend > .v-icon) {
  opacity: 0.9;
}

.menu-stat-badge {
  font-weight: 600;
  letter-spacing: 0.01em;
  border: 1px solid transparent;
}

.menu-stat-badge--info {
  color: #e7f7fc;
  background: rgba(68, 151, 176, 0.38);
  border-color: rgba(185, 225, 236, 0.38);
}

.menu-stat-badge--success {
  color: #ecfff0;
  background: rgba(61, 151, 93, 0.34);
  border-color: rgba(185, 235, 199, 0.38);
}

.nav-item--active {
  background: linear-gradient(
    135deg,
    rgba(180, 221, 233, 0.3),
    rgba(245, 252, 255, 0.22)
  );
  color: #ffffff;
  border: 1px solid rgba(206, 234, 241, 0.42);
  box-shadow: 0 10px 18px rgba(8, 24, 34, 0.3);
}

.sidenav-footer {
  border-top: 1px solid rgba(216, 238, 244, 0.18);
}

.sidenav-url-chip {
  width: 100%;
  justify-content: flex-start;
  color: rgba(239, 250, 252, 0.97);
  background: rgba(12, 37, 48, 0.54);
  border: 1px solid rgba(214, 239, 245, 0.35);
}

.sidenav-url-chip :deep(.v-chip__content) {
  color: rgba(239, 250, 252, 0.97);
}

.app-topbar {
  margin: 14px 30px 0 16px;
  border-radius: 18px;
  border: 1px solid rgba(var(--uav-secondary-rgb), 0.24);
  background: linear-gradient(
    140deg,
    rgba(246, 251, 252, 0.97),
    rgba(232, 243, 246, 0.95)
  );
  box-shadow: 0 14px 28px rgba(var(--uav-secondary-rgb), 0.16);
}

.topbar-overline {
  color: rgba(46, 99, 120, 0.78);
  letter-spacing: 0.08em;
  line-height: 1;
}

.topbar-title {
  font-size: 1.08rem;
  letter-spacing: 0.03em;
  text-transform: none;
  color: #275e73;
  line-height: 1.2;
}

.topbar-action {
  border: 1px solid rgba(var(--uav-secondary-rgb), 0.26);
}

.topbar-controls {
  margin-right: 22px;
}

.topbar-action :deep(.v-btn__content) {
  font-size: 0.9rem;
  font-weight: 600;
}

.topbar-chip {
  border: 1px solid rgba(var(--uav-secondary-rgb), 0.24);
  color: #1d4d60;
  background: rgba(222, 237, 242, 0.92);
}

.topbar-chip :deep(.v-chip__content) {
  font-size: 0.84rem;
  font-weight: 600;
}

.topbar-avatar-btn {
  border: 1px solid rgba(var(--uav-secondary-rgb), 0.24);
  background: rgba(250, 253, 253, 0.95);
}

@media (max-width: 960px) {
  .app-sidenav {
    backdrop-filter: blur(6px);
  }

  .menu-column {
    display: none;
  }

  .rail-column {
    width: 82px;
  }

  .app-topbar {
    margin: 8px 8px 0;
    border-radius: 12px;
  }

  .topbar-controls {
    margin-right: 2px;
    gap: 6px !important;
  }

  .topbar-action :deep(.v-btn__content),
  .topbar-chip :deep(.v-chip__content) {
    font-size: 0.8rem;
  }
}
</style>
