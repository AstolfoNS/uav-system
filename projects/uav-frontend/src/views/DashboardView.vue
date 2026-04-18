<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import { fetchNodePage, fetchRecordPage } from "@/api";
import type { YoloNode, YoloDetectionRecord } from "@/types";
import { notify } from "@/composables/useNotifier";
import PageHero from "@/components/PageHero.vue";

const { t } = useI18n();
const router = useRouter();

const loading = ref(false);
const isInitialLoadDone = ref(false);
const nodes = ref<YoloNode[]>([]);
const recentRecords = ref<YoloDetectionRecord[]>([]);

const sectionLoading = computed(
  () => !isInitialLoadDone.value && loading.value,
);
const sectionRefreshing = computed(
  () => isInitialLoadDone.value && loading.value,
);

function normalizeNodeStatus(status: unknown): number {
  if (typeof status === "number") {
    if (status === 1 || status === 2) {
      return status;
    }
    return 0;
  }

  const normalized = String(status ?? "")
    .trim()
    .toUpperCase();

  if (
    normalized === "1" ||
    normalized === "ONLINE" ||
    normalized === "ENABLED"
  ) {
    return 1;
  }

  if (
    normalized === "2" ||
    normalized === "EXCEPTION" ||
    normalized === "ERROR" ||
    normalized === "ABNORMAL"
  ) {
    return 2;
  }

  return 0;
}

function normalizeRecordStatus(status: unknown): number {
  if (typeof status === "number") {
    return status === 1 ? 1 : 0;
  }

  const normalized = String(status ?? "")
    .trim()
    .toUpperCase();

  if (
    normalized === "1" ||
    normalized === "SUCCESS" ||
    normalized === "SUCCEEDED" ||
    normalized === "OK" ||
    normalized === "COMPLETED" ||
    normalized === "DONE"
  ) {
    return 1;
  }

  return 0;
}

const onlineCount = computed(
  () =>
    nodes.value.filter((item) => normalizeNodeStatus(item.status) === 1).length,
);
const offlineCount = computed(
  () =>
    nodes.value.filter((item) => normalizeNodeStatus(item.status) === 0).length,
);
const exceptionCount = computed(
  () =>
    nodes.value.filter((item) => normalizeNodeStatus(item.status) === 2).length,
);
const totalRecords = ref(0);
const tableHeaders = computed(() => [
  { title: t("dashboard.table.id"), key: "id" },
  { title: t("dashboard.table.node"), key: "nodeId" },
  { title: t("dashboard.table.type"), key: "taskType" },
  { title: t("dashboard.table.filename"), key: "originalFilename" },
  { title: t("dashboard.table.status"), key: "status" },
]);

async function loadData(): Promise<void> {
  loading.value = true;
  try {
    const [nodePage, recordPage] = await Promise.all([
      fetchNodePage({ current: 1, size: 100 }),
      fetchRecordPage({ current: 1, size: 8 }),
    ]);

    nodes.value = nodePage.records ?? [];
    recentRecords.value = recordPage.records ?? [];
    totalRecords.value = Number(recordPage.total ?? 0);
  } catch (error) {
    const message =
      error instanceof Error ? error.message : t("dashboard.loadFailed");
    notify(message, "error");
  } finally {
    loading.value = false;
    isInitialLoadDone.value = true;
  }
}

async function goNodes(status: number | null): Promise<void> {
  if (status === null) {
    await router.push("/nodes");
    return;
  }

  await router.push({ path: "/nodes", query: { status: String(status) } });
}

async function goRecords(): Promise<void> {
  await router.push("/records");
}

onMounted(loadData);
</script>

<template>
  <div class="page-shell dashboard-page">
    <PageHero
      :title="t('app.menu.dashboard')"
      :subtitle="`${t('dashboard.nodesDesc')} · ${t('dashboard.recordsDesc')}`"
    >
      <template #actions>
        <v-btn
          class="btn-primary-action"
          color="primary"
          prepend-icon="mdi-refresh"
          variant="elevated"
          @click="loadData"
        >
          {{ t("common.apply") }}
        </v-btn>
      </template>
    </PageHero>

    <v-row class="metric-grid">
      <v-col cols="12" md="6" xl="3">
        <v-card
          class="card-ambient card-spacious balanced-card metric-card metric-card--sky"
          rounded="xl"
          :aria-label="t('dashboard.ariaViewAllNodes')"
          aria-describedby="dashboard-card-nodes-desc dashboard-card-nodes-hint"
          @click="goNodes(null)"
          @keydown.enter.prevent="goNodes(null)"
          @keydown.space.prevent="goNodes(null)"
          role="button"
          tabindex="0"
        >
          <v-card-text>
            <div class="text-overline">{{ t("dashboard.nodes") }}</div>
            <div class="text-h4 font-weight-bold mt-1">{{ nodes.length }}</div>
            <div id="dashboard-card-nodes-desc" class="text-caption mt-2">
              {{ t("dashboard.nodesDesc") }}
            </div>
            <div class="metric-hint text-caption mt-3">
              <v-icon size="14">mdi-open-in-new</v-icon>
              <span id="dashboard-card-nodes-hint">{{
                t("dashboard.clickHint")
              }}</span>
            </div>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="6" xl="3">
        <v-card
          class="card-ambient card-spacious balanced-card metric-card metric-card--cyan"
          rounded="xl"
          :aria-label="t('dashboard.ariaViewOnlineNodes')"
          aria-describedby="dashboard-card-online-desc dashboard-card-online-hint"
          @click="goNodes(1)"
          @keydown.enter.prevent="goNodes(1)"
          @keydown.space.prevent="goNodes(1)"
          role="button"
          tabindex="0"
        >
          <v-card-text>
            <div class="text-overline">{{ t("dashboard.online") }}</div>
            <div class="text-h4 font-weight-bold mt-1">{{ onlineCount }}</div>
            <div id="dashboard-card-online-desc" class="text-caption mt-2">
              {{ t("dashboard.onlineDesc") }}
            </div>
            <div class="metric-hint text-caption mt-3">
              <v-icon size="14">mdi-open-in-new</v-icon>
              <span id="dashboard-card-online-hint">{{
                t("dashboard.clickHint")
              }}</span>
            </div>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="6" xl="3">
        <v-card
          class="card-ambient card-spacious balanced-card metric-card metric-card--amber"
          rounded="xl"
          :aria-label="t('dashboard.ariaViewExceptionNodes')"
          aria-describedby="dashboard-card-exception-desc dashboard-card-exception-hint"
          @click="goNodes(2)"
          @keydown.enter.prevent="goNodes(2)"
          @keydown.space.prevent="goNodes(2)"
          role="button"
          tabindex="0"
        >
          <v-card-text>
            <div class="text-overline">{{ t("dashboard.exception") }}</div>
            <div class="text-h4 font-weight-bold mt-1">
              {{ exceptionCount }}
            </div>
            <div id="dashboard-card-exception-desc" class="text-caption mt-2">
              {{ t("dashboard.exceptionDesc") }}
            </div>
            <div class="metric-hint text-caption mt-3">
              <v-icon size="14">mdi-open-in-new</v-icon>
              <span id="dashboard-card-exception-hint">{{
                t("dashboard.clickHint")
              }}</span>
            </div>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="6" xl="3">
        <v-card
          class="card-ambient card-spacious balanced-card metric-card metric-card--blue"
          rounded="xl"
          :aria-label="t('dashboard.ariaViewRecords')"
          aria-describedby="dashboard-card-records-desc dashboard-card-records-hint"
          @click="goRecords"
          @keydown.enter.prevent="goRecords"
          @keydown.space.prevent="goRecords"
          role="button"
          tabindex="0"
        >
          <v-card-text>
            <div class="text-overline">{{ t("dashboard.records") }}</div>
            <div class="text-h4 font-weight-bold mt-1">{{ totalRecords }}</div>
            <div id="dashboard-card-records-desc" class="text-caption mt-2">
              {{ t("dashboard.recordsDesc") }}
            </div>
            <div class="metric-hint text-caption mt-3">
              <v-icon size="14">mdi-open-in-new</v-icon>
              <span id="dashboard-card-records-hint">{{
                t("dashboard.clickHint")
              }}</span>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-row>
      <v-col cols="12" lg="4">
        <v-card class="card-ambient card-spacious h-100" rounded="xl">
          <v-card-title class="d-flex align-center justify-space-between">
            {{ t("dashboard.nodeStatus") }}
            <v-btn
              icon="mdi-refresh"
              size="small"
              variant="text"
              @click="loadData"
            />
          </v-card-title>
          <v-divider />
          <v-card-text>
            <v-progress-linear
              v-if="sectionLoading || sectionRefreshing"
              class="mb-3"
              color="primary"
              indeterminate
              rounded
            />
            <v-list class="status-list" density="comfortable">
              <v-list-item
                prepend-icon="mdi-check-circle-outline"
                :title="t('dashboard.online')"
                :subtitle="String(onlineCount)"
              />
              <v-list-item
                prepend-icon="mdi-alert-circle-outline"
                :title="t('dashboard.offline')"
                :subtitle="String(offlineCount)"
              />
              <v-list-item
                prepend-icon="mdi-alert-octagon-outline"
                :title="t('dashboard.exception')"
                :subtitle="String(exceptionCount)"
              />
            </v-list>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" lg="8">
        <v-card class="card-ambient card-spacious" rounded="xl">
          <v-card-title>{{ t("dashboard.recentRecords") }}</v-card-title>
          <v-divider />
          <v-card-text class="table-shell">
            <v-data-table
              :headers="tableHeaders"
              :items="recentRecords"
              :loading="sectionLoading"
              :loading-text="t('common.loading')"
              :no-data-text="t('dashboard.recentRecordsEmpty')"
              density="comfortable"
              item-value="id"
            >
              <template #item.taskType="{ item }">
                <v-chip size="small" variant="tonal">
                  {{
                    Number(item.taskType) === 2
                      ? t("dashboard.typeVideo")
                      : t("dashboard.typeImage")
                  }}
                </v-chip>
              </template>
              <template #item.status="{ item }">
                <v-chip
                  :color="
                    normalizeRecordStatus(item.status) === 1
                      ? 'success'
                      : 'warning'
                  "
                  size="small"
                  variant="flat"
                >
                  {{
                    normalizeRecordStatus(item.status) === 1
                      ? t("dashboard.ok")
                      : t("dashboard.failed")
                  }}
                </v-chip>
              </template>
            </v-data-table>
            <v-progress-linear
              v-if="sectionRefreshing"
              class="mt-2"
              color="primary"
              indeterminate
              rounded
            />
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<style scoped>
.metric-card {
  cursor: pointer;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
  min-height: 192px;
}

.metric-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 18px 30px rgba(var(--uav-primary-rgb), 0.22);
}

.metric-card:focus-visible {
  outline: 2px solid rgba(var(--uav-primary-rgb), 0.88);
  outline-offset: 2px;
}

.metric-hint {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  opacity: 0.78;
}

.metric-grid {
  row-gap: 20px;
}

.metric-card--sky {
  background: linear-gradient(
    145deg,
    rgba(226, 248, 255, 0.97),
    rgba(243, 253, 255, 0.94)
  );
}

.metric-card--cyan {
  background: linear-gradient(
    145deg,
    rgba(224, 251, 255, 0.97),
    rgba(242, 255, 255, 0.94)
  );
}

.metric-card--amber {
  background: linear-gradient(
    145deg,
    rgba(233, 248, 255, 0.97),
    rgba(244, 252, 255, 0.95)
  );
}

.metric-card--blue {
  background: linear-gradient(
    145deg,
    rgba(222, 245, 255, 0.98),
    rgba(239, 250, 255, 0.95)
  );
}

.status-list {
  border: 1px dashed rgba(var(--uav-primary-rgb), 0.24);
  border-radius: 14px;
  background: rgba(247, 253, 255, 0.82);
}

.table-shell {
  padding-top: 16px;
}
</style>
