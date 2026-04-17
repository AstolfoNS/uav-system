<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import {
  fetchNodePage,
  fetchRecordDetail,
  fetchRecordPage,
  removeRecord,
} from "@/api";
import { useScrollEdgeState } from "@/composables/useScrollEdgeState";
import type { YoloDetectionRecord, YoloNode } from "@/types";
import { notify } from "@/composables/useNotifier";
import ConfirmActionDialog from "@/components/ConfirmActionDialog.vue";
import PageHero from "@/components/PageHero.vue";

const { t } = useI18n();

const loading = ref(false);
const isInitialLoadDone = ref(false);
const records = ref<YoloDetectionRecord[]>([]);
const total = ref(0);
const current = ref(1);
const size = ref(10);

const nodes = ref<YoloNode[]>([]);

const filters = reactive({
  nodeId: null as number | null,
  taskType: null as number | null,
  originalFilename: "",
});

const detailDialog = ref(false);
const detailLoading = ref(false);
const detail = ref<YoloDetectionRecord | null>(null);
const previewDialog = ref(false);
const previewKind = ref<"image" | "video">("image");
const previewSrc = ref("");
const previewTitle = ref("");
const showAllRecordDetails = ref(false);
const confirmDeleteDialog = ref(false);
const deleting = ref(false);
const pendingDeleteRecordId = ref<number | null>(null);
const recordDetailScrollRef = ref<HTMLElement | null>(null);

const { scrollState, refreshByElement, handleScroll } = useScrollEdgeState(
  ["recordDetail"] as const,
  { threshold: 3 },
);

const typeItems = computed(() => [
  { title: t("records.all"), value: null },
  { title: t("records.image"), value: 1 },
  { title: t("records.video"), value: 2 },
]);

const tableLoading = computed(() => !isInitialLoadDone.value && loading.value);
const tableRefreshing = computed(
  () => isInitialLoadDone.value && loading.value,
);
const suppressTaskTypeAutoLoad = ref(false);

function getPageScrollTop(): number {
  return (
    window.scrollY ||
    window.pageYOffset ||
    document.documentElement.scrollTop ||
    0
  );
}

async function restorePageScrollTop(scrollTop: number): Promise<void> {
  await nextTick();
  window.scrollTo({ top: scrollTop, left: 0 });
}

function normalizeTaskType(taskType: unknown): number | null {
  if (typeof taskType === "number") {
    return taskType === 2 ? 2 : 1;
  }

  const normalized = String(taskType ?? "")
    .trim()
    .toUpperCase();

  if (normalized === "2" || normalized === "VIDEO") {
    return 2;
  }

  if (normalized === "1" || normalized === "IMAGE" || normalized === "IMG") {
    return 1;
  }

  return null;
}

const selectedTaskType = computed(() => normalizeTaskType(filters.taskType));

const filteredRecords = computed(() => {
  const currentTaskType = selectedTaskType.value;
  if (currentTaskType === null) {
    return records.value;
  }

  return records.value.filter(
    (item) => normalizeTaskType(item.taskType) === currentTaskType,
  );
});

const pageCount = computed(() =>
  Math.max(1, Math.ceil(total.value / size.value)),
);

const successCountInPage = computed(
  () =>
    filteredRecords.value.filter(
      (item) => normalizeRecordStatus(item.status) === 1,
    ).length,
);

const failedCountInPage = computed(
  () =>
    filteredRecords.value.filter(
      (item) => normalizeRecordStatus(item.status) !== 1,
    ).length,
);

const imageCountInPage = computed(
  () =>
    filteredRecords.value.filter(
      (item) => normalizeTaskType(item.taskType) !== 2,
    ).length,
);

const videoCountInPage = computed(
  () =>
    filteredRecords.value.filter(
      (item) => normalizeTaskType(item.taskType) === 2,
    ).length,
);

const headers = computed(() => [
  { title: t("records.table.id"), key: "id", width: 90 },
  { title: t("records.table.node"), key: "nodeId", width: 90 },
  { title: t("records.table.type"), key: "taskType", width: 120 },
  { title: t("records.table.filename"), key: "originalFilename" },
  { title: t("records.table.status"), key: "status", width: 110 },
  { title: t("records.table.createdAt"), key: "createdAt", width: 200 },
  {
    title: t("records.table.actions"),
    key: "actions",
    sortable: false,
    width: 220,
  },
]);

function asObject(value: unknown): Record<string, unknown> {
  if (value && typeof value === "object") {
    return value as Record<string, unknown>;
  }

  return {};
}

function pickMediaUrl(
  record: YoloDetectionRecord | null,
  keys: string[],
): string {
  if (!record) {
    return "";
  }

  const bag = asObject(record);
  for (const key of keys) {
    const raw = bag[key];
    if (typeof raw === "string" && raw.trim().startsWith("http")) {
      return raw.trim();
    }
  }

  const detailBag = asObject(bag.detectionDetails);
  for (const key of keys) {
    const raw = detailBag[key];
    if (typeof raw === "string" && raw.trim().startsWith("http")) {
      return raw.trim();
    }
  }

  return "";
}

function previewUrlFor(record: YoloDetectionRecord): string {
  if (Number(record.taskType) === 2) {
    return pickMediaUrl(record, [
      "resultUrl",
      "result_url",
      "videoUrl",
      "video_url",
      "resultVideoUrl",
      "renderedVideoUrl",
      "sourceUrl",
      "source_url",
      "inputVideoUrl",
      "originVideoUrl",
    ]);
  }

  return pickMediaUrl(record, [
    "resultUrl",
    "result_url",
    "imageUrl",
    "image_url",
    "resultImageUrl",
    "renderedImageUrl",
    "sourceUrl",
    "source_url",
    "inputImageUrl",
    "originImageUrl",
  ]);
}

function openPreview(record: YoloDetectionRecord): void {
  const src = previewUrlFor(record);
  if (!src) {
    notify(t("records.previewUnavailable"), "warning");
    return;
  }

  previewKind.value = Number(record.taskType) === 2 ? "video" : "image";
  previewSrc.value = src;
  previewTitle.value = String(record.originalFilename ?? t("records.detail"));
  previewDialog.value = true;
}

function closePreview(): void {
  previewDialog.value = false;
}

function fileNameFromUrl(url: string): string {
  try {
    const parsed = new URL(url);
    const path = parsed.pathname.split("/").filter(Boolean);
    const last = path[path.length - 1] ?? "";
    return decodeURIComponent(last);
  } catch {
    return "";
  }
}

function downloadRecordMedia(record: YoloDetectionRecord): void {
  const src = previewUrlFor(record);
  if (!src) {
    notify(t("records.downloadUnavailable"), "warning");
    return;
  }

  const fallback =
    String(record.originalFilename ?? "").trim() ||
    fileNameFromUrl(src) ||
    "record-media";

  const anchor = document.createElement("a");
  anchor.href = src;
  anchor.download = fallback;
  anchor.target = "_blank";
  anchor.rel = "noopener noreferrer";
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
}

async function loadNodes(): Promise<void> {
  try {
    const page = await fetchNodePage({ current: 1, size: 100 });
    nodes.value = page.records ?? [];
  } catch {
    // optional node options; ignore errors here
  }
}

async function loadRecords(options?: {
  preserveScrollPosition?: boolean;
}): Promise<void> {
  const preservedScrollTop = options?.preserveScrollPosition
    ? getPageScrollTop()
    : null;

  loading.value = true;
  try {
    const page = await fetchRecordPage({
      current: current.value,
      size: size.value,
      nodeId: filters.nodeId,
      taskType: selectedTaskType.value ?? undefined,
      originalFilename: filters.originalFilename.trim() || undefined,
    });

    records.value = page.records ?? [];
    total.value = Number(page.total ?? 0);
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("records.loadFailed"),
      "error",
    );
  } finally {
    loading.value = false;
    isInitialLoadDone.value = true;

    if (preservedScrollTop !== null) {
      await restorePageScrollTop(preservedScrollTop);
    }
  }
}

async function resetFilters(): Promise<void> {
  suppressTaskTypeAutoLoad.value = true;
  try {
    filters.nodeId = null;
    filters.taskType = null;
    filters.originalFilename = "";
    current.value = 1;
    await loadRecords({ preserveScrollPosition: true });
  } finally {
    suppressTaskTypeAutoLoad.value = false;
  }
}

async function openDetail(recordId: number): Promise<void> {
  detailDialog.value = true;
  detailLoading.value = true;
  try {
    detail.value = await fetchRecordDetail(recordId);
    showAllRecordDetails.value = false;
  } catch (error) {
    detail.value = null;
    notify(
      error instanceof Error ? error.message : t("records.detailFailed"),
      "error",
    );
  } finally {
    detailLoading.value = false;
  }
}

async function remove(recordId: number): Promise<void> {
  pendingDeleteRecordId.value = recordId;
  confirmDeleteDialog.value = true;
}

async function confirmDelete(): Promise<void> {
  if (!pendingDeleteRecordId.value || deleting.value) {
    return;
  }

  try {
    deleting.value = true;
    await removeRecord(pendingDeleteRecordId.value);
    notify(t("records.deleteSuccess"), "success");
    confirmDeleteDialog.value = false;
    pendingDeleteRecordId.value = null;
    await loadRecords();
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("records.deleteFailed"),
      "error",
    );
  } finally {
    deleting.value = false;
  }
}

function typeLabel(taskType: number | undefined): string {
  return normalizeTaskType(taskType) === 2
    ? t("records.typeVideo")
    : t("records.typeImage");
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

function statusLabel(status: unknown): string {
  return normalizeRecordStatus(status) === 1
    ? t("records.statusOk")
    : t("records.statusFailed");
}

function statusColor(status: unknown): string {
  return normalizeRecordStatus(status) === 1 ? "success" : "warning";
}

function asText(value: unknown): string {
  if (value === null || value === undefined || value === "") {
    return t("common.unavailable");
  }
  if (typeof value === "object") {
    try {
      return JSON.stringify(value, null, 2);
    } catch {
      return String(value);
    }
  }
  return String(value);
}

function isStructuredJsonValue(value: string): boolean {
  return value.startsWith("{\n") || value.startsWith("[\n");
}

function detailRows(
  record: YoloDetectionRecord,
): Array<{ label: string; value: string }> {
  return [
    { label: t("records.table.id"), value: asText(record.id) },
    { label: t("records.table.node"), value: asText(record.nodeId) },
    { label: t("records.table.type"), value: typeLabel(record.taskType) },
    { label: t("records.table.status"), value: statusLabel(record.status) },
    {
      label: t("records.table.filename"),
      value: asText(record.originalFilename),
    },
    {
      label: t("records.table.createdAt"),
      value: asText(record.createdAt),
    },
    {
      label: t("records.fieldUpdatedAt"),
      value: asText(record.updatedAt),
    },
    {
      label: t("records.fieldErrorMessage"),
      value: asText(record.errorMessage),
    },
  ];
}

function detailItemCount(record: YoloDetectionRecord): number {
  const bag =
    record.detectionDetails && typeof record.detectionDetails === "object"
      ? (record.detectionDetails as Record<string, unknown>)
      : {};
  return Object.keys(bag).length;
}

function detailItemsByMode(
  record: YoloDetectionRecord,
  showAll: boolean,
): Array<{ key: string; value: string }> {
  const bag =
    record.detectionDetails && typeof record.detectionDetails === "object"
      ? (record.detectionDetails as Record<string, unknown>)
      : {};
  const entries = Object.entries(bag);
  const target = showAll ? entries : entries.slice(0, 12);
  return target.map(([key, value]) => ({ key, value: asText(value) }));
}

function summaryText(record: YoloDetectionRecord): string {
  return detailRows(record)
    .map((row) => `${row.label}: ${row.value}`)
    .join("\n");
}

async function copyRecordSummary(record: YoloDetectionRecord): Promise<void> {
  try {
    await navigator.clipboard.writeText(summaryText(record));
    notify(t("records.copiedSummary"), "success");
  } catch {
    notify(t("records.copySummaryFailed"), "error");
  }
}

function refreshRecordDetailScrollState(): void {
  refreshByElement("recordDetail", recordDetailScrollRef.value);
}

onMounted(async () => {
  await Promise.all([loadNodes(), loadRecords()]);
});

watch(
  () => filters.taskType,
  async (newValue, oldValue) => {
    if (suppressTaskTypeAutoLoad.value || newValue === oldValue) {
      return;
    }

    current.value = 1;
    await loadRecords({ preserveScrollPosition: true });
  },
);

watch(
  () => [
    detailDialog.value,
    detailLoading.value,
    detail.value,
    showAllRecordDetails.value,
  ],
  async () => {
    await nextTick();
    refreshRecordDetailScrollState();
  },
  { flush: "post" },
);
</script>

<template>
  <div class="page-shell records-page">
    <PageHero
      :title="t('records.title')"
      :subtitle="`${t('records.table.filename')} · ${t('records.table.createdAt')}`"
    />

    <v-card class="card-ambient card-spacious" rounded="xl">
      <v-card-text>
        <v-row class="filter-row">
          <v-col cols="12" md="3">
            <v-select
              v-model="filters.nodeId"
              :items="[{ nodeName: t('records.allNodes'), id: null }, ...nodes]"
              item-title="nodeName"
              item-value="id"
              :label="t('records.node')"
              variant="outlined"
            />
          </v-col>
          <v-col cols="12" md="3">
            <v-select
              v-model="filters.taskType"
              :items="typeItems"
              item-title="title"
              item-value="value"
              :label="t('records.taskType')"
              variant="outlined"
            />
          </v-col>
          <v-col cols="12" md="6">
            <v-text-field
              v-model="filters.originalFilename"
              clearable
              :label="t('records.filename')"
              prepend-inner-icon="mdi-file-search"
              variant="outlined"
            />
          </v-col>
        </v-row>
        <v-row class="filter-actions-row">
          <v-col cols="12" class="filter-actions-bar">
            <v-btn
              class="btn-primary-action"
              color="primary"
              prepend-icon="mdi-filter"
              @click="
                current = 1;
                loadRecords();
              "
              >{{ t("common.apply") }}</v-btn
            >
            <v-btn
              class="btn-secondary-action"
              prepend-icon="mdi-refresh"
              variant="tonal"
              @click="resetFilters"
            >
              {{ t("common.reset") }}
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <v-card class="card-ambient card-spacious" rounded="xl">
      <v-card-text>
        <div class="summary-chip-wall">
          <v-chip color="success" variant="flat"
            >{{ t("records.statusOk") }} · {{ successCountInPage }}</v-chip
          >
          <v-chip color="warning" variant="flat"
            >{{ t("records.statusFailed") }} · {{ failedCountInPage }}</v-chip
          >
          <v-chip color="info" variant="tonal"
            >{{ t("records.typeImage") }} · {{ imageCountInPage }}</v-chip
          >
          <v-chip color="primary" variant="tonal"
            >{{ t("records.typeVideo") }} · {{ videoCountInPage }}</v-chip
          >
        </div>
      </v-card-text>
    </v-card>

    <v-card class="card-ambient card-spacious" rounded="xl">
      <v-card-text class="table-shell">
        <v-data-table
          :headers="headers"
          :items="filteredRecords"
          :loading="tableLoading"
          :loading-text="t('common.loading')"
          :no-data-text="t('records.tableEmpty')"
          density="comfortable"
          item-value="id"
        >
          <template #item.taskType="{ item }">
            <v-chip size="small" variant="tonal">{{
              typeLabel(Number(item.taskType))
            }}</v-chip>
          </template>

          <template #item.status="{ item }">
            <v-chip
              :color="statusColor(item.status)"
              size="small"
              variant="flat"
            >
              {{ statusLabel(item.status) }}
            </v-chip>
          </template>

          <template #item.actions="{ item }">
            <div class="d-flex ga-1">
              <v-btn
                :title="t('records.actionPreview')"
                icon="mdi-image-search-outline"
                size="x-small"
                variant="text"
                @click="openPreview(item)"
              />
              <v-btn
                :title="t('records.actionDownload')"
                icon="mdi-download"
                size="x-small"
                variant="text"
                @click="downloadRecordMedia(item)"
              />
              <v-btn
                :title="t('records.detail')"
                icon="mdi-text-box-search-outline"
                size="x-small"
                variant="text"
                @click="openDetail(Number(item.id))"
              />
              <v-btn
                icon="mdi-delete"
                size="x-small"
                variant="text"
                @click="remove(Number(item.id))"
              />
            </div>
          </template>
        </v-data-table>

        <v-progress-linear
          v-if="tableRefreshing"
          class="mt-2"
          color="primary"
          indeterminate
          rounded
        />

        <div class="d-flex justify-end mt-4">
          <v-pagination
            v-model="current"
            :length="pageCount"
            :total-visible="7"
            @update:model-value="loadRecords"
          />
        </div>
      </v-card-text>
    </v-card>
  </div>

  <v-dialog v-model="previewDialog" max-width="1200">
    <v-card class="preview-dialog-card" rounded="xl">
      <v-card-title class="d-flex align-center ga-2">
        <span class="text-subtitle-1">{{ previewTitle }}</span>
        <v-spacer />
        <v-btn icon="mdi-close" variant="text" @click="closePreview" />
      </v-card-title>
      <v-divider />
      <v-card-text class="preview-dialog-body">
        <v-img
          v-if="previewKind === 'image'"
          class="preview-dialog-image"
          :src="previewSrc"
          contain
        />
        <video v-else class="preview-dialog-video" controls autoplay>
          <source :src="previewSrc" />
        </video>
      </v-card-text>
    </v-card>
  </v-dialog>

  <v-dialog v-model="detailDialog" max-width="860">
    <v-card rounded="xl">
      <v-card-title>{{ t("records.detail") }}</v-card-title>
      <v-divider />
      <v-card-text>
        <v-progress-linear
          v-if="detailLoading"
          color="primary"
          indeterminate
          rounded
        />
        <template v-else>
          <v-card v-if="detail" variant="tonal">
            <v-card-title class="d-flex align-center ga-2">
              {{ t("records.detailSummary") }}
              <v-spacer />
              <v-btn
                color="primary"
                size="small"
                variant="text"
                @click="copyRecordSummary(detail)"
              >
                {{ t("common.copy") }}
              </v-btn>
            </v-card-title>
            <v-card-text>
              <v-row dense>
                <v-col
                  v-for="row in detailRows(detail)"
                  :key="`record-${row.label}`"
                  cols="12"
                  md="6"
                >
                  <div class="text-caption text-medium-emphasis">
                    {{ row.label }}
                  </div>
                  <div
                    class="text-body-2 detail-value"
                    :class="{
                      'detail-value--json': isStructuredJsonValue(row.value),
                    }"
                  >
                    {{ row.value }}
                  </div>
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>
          <v-card v-if="detail" class="mt-3" variant="outlined">
            <v-card-title class="d-flex align-center ga-2">
              {{ t("records.detailItemsTitle") }}
              <v-spacer />
              <v-btn
                v-if="detailItemCount(detail) > 12"
                color="primary"
                size="small"
                variant="text"
                @click="showAllRecordDetails = !showAllRecordDetails"
              >
                {{
                  showAllRecordDetails
                    ? t("common.showLess")
                    : t("common.showAll")
                }}
              </v-btn>
            </v-card-title>
            <v-card-text>
              <div
                ref="recordDetailScrollRef"
                class="record-detail-scroll scroll-elegant scroll-edge-hint"
                :class="{
                  'scroll-at-top': scrollState.recordDetail.atTop,
                  'scroll-at-bottom': scrollState.recordDetail.atBottom,
                }"
                @scroll.passive="handleScroll('recordDetail', $event)"
              >
                <v-row
                  v-if="
                    detailItemsByMode(detail, showAllRecordDetails).length > 0
                  "
                  dense
                >
                  <v-col
                    v-for="item in detailItemsByMode(
                      detail,
                      showAllRecordDetails,
                    )"
                    :key="`record-detail-${item.key}`"
                    cols="12"
                    md="6"
                  >
                    <div class="text-caption text-medium-emphasis mono">
                      {{ item.key }}
                    </div>
                    <div
                      class="text-body-2 detail-value"
                      :class="{
                        'detail-value--json': isStructuredJsonValue(item.value),
                      }"
                    >
                      {{ item.value }}
                    </div>
                  </v-col>
                </v-row>
                <div v-else class="text-body-2 text-medium-emphasis">
                  {{ t("records.detailItemsEmpty") }}
                </div>
              </div>
            </v-card-text>
          </v-card>
          <div v-if="!detail" class="text-body-2 text-medium-emphasis">
            {{ t("records.noDetailData") }}
          </div>
        </template>
      </v-card-text>
    </v-card>
  </v-dialog>

  <ConfirmActionDialog
    v-model="confirmDeleteDialog"
    :cancel-text="t('common.cancel')"
    :confirm-text="t('common.delete')"
    :loading="deleting"
    :message="t('records.deleteConfirm')"
    :title="t('records.deleteTitle')"
    @confirm="confirmDelete"
  />
</template>

<style scoped>
.summary-chip-wall {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-row {
  row-gap: 8px;
}

.filter-actions-row {
  margin-top: -2px;
}

.filter-actions-bar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.table-shell {
  padding-top: 16px;
}

.record-detail-scroll {
  max-height: 320px;
  overflow: auto;
  padding-right: 4px;
}

.detail-value {
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-value--json {
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 0.82rem;
  line-height: 1.35;
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(21, 58, 73, 0.08);
  border: 1px solid rgba(31, 122, 140, 0.2);
}

.preview-dialog-card {
  background: linear-gradient(
    145deg,
    rgba(238, 247, 249, 0.98),
    rgba(247, 251, 252, 0.98)
  );
}

.preview-dialog-body {
  padding: 20px;
}

.preview-dialog-image,
.preview-dialog-video {
  width: 100%;
  max-height: 70vh;
  border-radius: 14px;
  background: rgba(8, 24, 33, 0.92);
}

@media (max-width: 960px) {
  .filter-actions-bar {
    justify-content: stretch;
  }

  .filter-actions-bar > .v-btn {
    width: 100%;
  }
}
</style>
