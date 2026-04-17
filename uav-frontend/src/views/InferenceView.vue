<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { fetchNodePage, predictImage, predictVideo } from "@/api";
import { useScrollEdgeState } from "@/composables/useScrollEdgeState";
import type { YoloNode, YoloDetectionRecord } from "@/types";
import { notify } from "@/composables/useNotifier";
import InferenceNodeSelectorCard from "@/components/InferenceNodeSelectorCard.vue";
import PageHero from "@/components/PageHero.vue";

const { t } = useI18n();

const nodes = ref<YoloNode[]>([]);
const selectedNodeId = ref<number | null>(null);

const imageFile = ref<File | null>(null);
const videoFile = ref<File | null>(null);

const imageLoading = ref(false);
const videoLoading = ref(false);

const imageResult = ref<YoloDetectionRecord | null>(null);
const videoResult = ref<YoloDetectionRecord | null>(null);
const imageInputPreviewUrl = ref("");
const videoInputPreviewUrl = ref("");
const imageFlowHighlighted = ref(false);
const videoFlowHighlighted = ref(false);
const showAllImageDetails = ref(false);
const showAllVideoDetails = ref(false);

const IMAGE_MAX_SIZE_BYTES = 10 * 1024 * 1024;
const VIDEO_MAX_SIZE_BYTES = 200 * 1024 * 1024;
const VIDEO_ALLOWED_EXTENSIONS = [".mp4", ".avi", ".mov", ".mkv"];

const canSubmitImage = ref(true);
const canSubmitVideo = ref(true);
const activeTask = ref<"image" | "video">("image");
let flowPulseTimer: ReturnType<typeof setTimeout> | null = null;

const scrollPanelKeys = [
  "imageSummary",
  "imageDetail",
  "videoSummary",
  "videoDetail",
] as const;

type ScrollPanelKey = (typeof scrollPanelKeys)[number];

const imageSummaryScrollRef = ref<HTMLElement | null>(null);
const imageDetailScrollRef = ref<HTMLElement | null>(null);
const videoSummaryScrollRef = ref<HTMLElement | null>(null);
const videoDetailScrollRef = ref<HTMLElement | null>(null);

const { scrollState, refreshAllByResolver, handleScroll } = useScrollEdgeState(
  scrollPanelKeys,
  { threshold: 1 },
);

function resolveScrollEl(key: ScrollPanelKey): HTMLElement | null {
  switch (key) {
    case "imageSummary":
      return imageSummaryScrollRef.value;
    case "imageDetail":
      return imageDetailScrollRef.value;
    case "videoSummary":
      return videoSummaryScrollRef.value;
    case "videoDetail":
      return videoDetailScrollRef.value;
    default:
      return null;
  }
}

function refreshAllScrollState(): void {
  refreshAllByResolver(resolveScrollEl);
}

function pulseFlow(task: "image" | "video"): void {
  if (flowPulseTimer) {
    clearTimeout(flowPulseTimer);
  }

  imageFlowHighlighted.value = task === "image";
  videoFlowHighlighted.value = task === "video";

  flowPulseTimer = setTimeout(() => {
    imageFlowHighlighted.value = false;
    videoFlowHighlighted.value = false;
    flowPulseTimer = null;
  }, 900);
}

function getFileExtension(filename: string): string {
  const index = filename.lastIndexOf(".");
  if (index < 0) {
    return "";
  }
  return filename.slice(index).toLowerCase();
}

function validateImage(file: File): boolean {
  if (!file.type.startsWith("image/")) {
    notify(t("inference.imageTypeInvalid"), "warning");
    return false;
  }

  if (file.size > IMAGE_MAX_SIZE_BYTES) {
    notify(t("inference.imageTooLarge"), "warning");
    return false;
  }

  return true;
}

function validateVideo(file: File): boolean {
  const extension = getFileExtension(file.name);
  const byType = file.type.startsWith("video/");
  const byExt = VIDEO_ALLOWED_EXTENSIONS.includes(extension);

  if (!byType && !byExt) {
    notify(t("inference.videoTypeInvalid"), "warning");
    return false;
  }

  if (file.size > VIDEO_MAX_SIZE_BYTES) {
    notify(t("inference.videoTooLarge"), "warning");
    return false;
  }

  return true;
}

function onImageSelected(file: File | File[] | null): void {
  const next = Array.isArray(file) ? (file[0] ?? null) : file;
  imageFile.value = next;

  if (!next) {
    canSubmitImage.value = false;
    if (imageInputPreviewUrl.value) {
      URL.revokeObjectURL(imageInputPreviewUrl.value);
      imageInputPreviewUrl.value = "";
    }
    return;
  }

  const valid = validateImage(next);
  canSubmitImage.value = valid;
  if (!valid) {
    if (imageInputPreviewUrl.value) {
      URL.revokeObjectURL(imageInputPreviewUrl.value);
      imageInputPreviewUrl.value = "";
    }
    imageFile.value = null;
    return;
  }

  if (imageInputPreviewUrl.value) {
    URL.revokeObjectURL(imageInputPreviewUrl.value);
  }
  imageInputPreviewUrl.value = URL.createObjectURL(next);
}

function onVideoSelected(file: File | File[] | null): void {
  const next = Array.isArray(file) ? (file[0] ?? null) : file;
  videoFile.value = next;

  if (!next) {
    canSubmitVideo.value = false;
    if (videoInputPreviewUrl.value) {
      URL.revokeObjectURL(videoInputPreviewUrl.value);
      videoInputPreviewUrl.value = "";
    }
    return;
  }

  const valid = validateVideo(next);
  canSubmitVideo.value = valid;
  if (!valid) {
    if (videoInputPreviewUrl.value) {
      URL.revokeObjectURL(videoInputPreviewUrl.value);
      videoInputPreviewUrl.value = "";
    }
    videoFile.value = null;
    return;
  }

  if (videoInputPreviewUrl.value) {
    URL.revokeObjectURL(videoInputPreviewUrl.value);
  }
  videoInputPreviewUrl.value = URL.createObjectURL(next);
}

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
    if (typeof raw === "string" && raw.startsWith("http")) {
      return raw;
    }
  }

  const detail = asObject(bag.detectionDetails);
  for (const key of keys) {
    const raw = detail[key];
    if (typeof raw === "string" && raw.startsWith("http")) {
      return raw;
    }
  }

  return "";
}

function asText(value: unknown): string {
  if (value === null || value === undefined || value === "") {
    return t("common.unavailable");
  }
  if (typeof value === "object") {
    return JSON.stringify(value);
  }
  return String(value);
}

function typeLabel(taskType: unknown): string {
  return Number(taskType) === 2
    ? t("inference.typeVideo")
    : t("inference.typeImage");
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
    ? t("inference.statusOk")
    : t("inference.statusFailed");
}

function statusColor(status: unknown): string {
  return normalizeRecordStatus(status) === 1 ? "success" : "warning";
}

function resultRows(
  record: YoloDetectionRecord,
): Array<{ label: string; value: string }> {
  return [
    { label: t("inference.fieldId"), value: asText(record.id) },
    { label: t("inference.fieldNode"), value: asText(record.nodeId) },
    {
      label: t("inference.fieldType"),
      value: typeLabel(record.taskType),
    },
    {
      label: t("inference.fieldStatus"),
      value: statusLabel(record.status),
    },
    {
      label: t("inference.fieldFilename"),
      value: asText(record.originalFilename),
    },
    {
      label: t("inference.fieldCreatedAt"),
      value: asText(record.createdAt),
    },
    {
      label: t("inference.fieldUpdatedAt"),
      value: asText(record.updatedAt),
    },
    {
      label: t("inference.fieldErrorMessage"),
      value: asText(record.errorMessage),
    },
  ];
}

function detailRowCount(record: YoloDetectionRecord): number {
  return Object.keys(asObject(record.detectionDetails)).length;
}

function detailRowsByMode(
  record: YoloDetectionRecord,
  showAll: boolean,
): Array<{ key: string; value: string }> {
  const details = asObject(record.detectionDetails);
  const entries = Object.entries(details);
  const target = showAll ? entries : entries.slice(0, 12);
  return target.map(([key, value]) => ({ key, value: asText(value) }));
}

function summaryText(record: YoloDetectionRecord): string {
  return resultRows(record)
    .map((row) => `${row.label}: ${row.value}`)
    .join("\n");
}

async function copySummary(record: YoloDetectionRecord): Promise<void> {
  try {
    await navigator.clipboard.writeText(summaryText(record));
    notify(t("inference.copiedSummary"), "success");
  } catch {
    notify(t("inference.copySummaryFailed"), "error");
  }
}

async function loadNodes(): Promise<void> {
  try {
    const page = await fetchNodePage({ current: 1, size: 100, status: 1 });
    nodes.value = page.records ?? [];
    if (!selectedNodeId.value && nodes.value.length > 0) {
      selectedNodeId.value = Number(nodes.value[0].id);
    }
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("inference.loadNodeFailed"),
      "error",
    );
  }
}

async function submitImage(): Promise<void> {
  if (imageLoading.value) {
    return;
  }

  if (!selectedNodeId.value) {
    notify(t("inference.nodeRequired"), "warning");
    return;
  }

  if (!imageFile.value) {
    notify(t("inference.imageRequired"), "warning");
    return;
  }

  if (!validateImage(imageFile.value)) {
    return;
  }

  imageLoading.value = true;
  try {
    imageResult.value = await predictImage(
      selectedNodeId.value,
      imageFile.value,
    );
    showAllImageDetails.value = false;
    pulseFlow("image");
    notify(t("inference.imageSuccess"), "success");
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("inference.imageFailed"),
      "error",
    );
  } finally {
    imageLoading.value = false;
  }
}

async function submitVideo(): Promise<void> {
  if (videoLoading.value) {
    return;
  }

  if (!selectedNodeId.value) {
    notify(t("inference.nodeRequired"), "warning");
    return;
  }

  if (!videoFile.value) {
    notify(t("inference.videoRequired"), "warning");
    return;
  }

  if (!validateVideo(videoFile.value)) {
    return;
  }

  videoLoading.value = true;
  try {
    videoResult.value = await predictVideo(
      selectedNodeId.value,
      videoFile.value,
    );
    showAllVideoDetails.value = false;
    pulseFlow("video");
    notify(t("inference.videoSuccess"), "success");
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("inference.videoFailed"),
      "error",
    );
  } finally {
    videoLoading.value = false;
  }
}

onMounted(loadNodes);

watch(
  () => [
    imageResult.value,
    videoResult.value,
    showAllImageDetails.value,
    showAllVideoDetails.value,
  ],
  async () => {
    await nextTick();
    refreshAllScrollState();
  },
  { flush: "post" },
);

onMounted(() => {
  void nextTick().then(() => {
    refreshAllScrollState();
  });
});

onBeforeUnmount(() => {
  if (flowPulseTimer) {
    clearTimeout(flowPulseTimer);
    flowPulseTimer = null;
  }

  if (imageInputPreviewUrl.value) {
    URL.revokeObjectURL(imageInputPreviewUrl.value);
  }
  if (videoInputPreviewUrl.value) {
    URL.revokeObjectURL(videoInputPreviewUrl.value);
  }
});
</script>

<template>
  <div class="page-shell inference-page">
    <PageHero
      :title="t('inference.title')"
      :subtitle="`${t('inference.imageTitle')} / ${t('inference.videoTitle')}`"
    />

    <v-row justify="center">
      <v-col cols="12" xl="10" class="task-card-col">
        <v-card class="card-ambient card-spacious task-card" rounded="xl">
          <v-tabs v-model="activeTask" class="task-tabs" color="primary" grow>
            <v-tab value="image">{{ t("inference.imageTitle") }}</v-tab>
            <v-tab value="video">{{ t("inference.videoTitle") }}</v-tab>
          </v-tabs>
          <v-divider />
          <v-window v-model="activeTask">
            <v-window-item value="image">
              <v-card-text class="task-panel">
                <v-row class="task-layout" align="start">
                  <v-col cols="12" lg="3">
                    <InferenceNodeSelectorCard
                      v-model="selectedNodeId"
                      :nodes="nodes"
                    />
                  </v-col>

                  <v-col cols="12" lg="9">
                    <v-file-input
                      class="task-input"
                      :model-value="imageFile"
                      accept="image/*"
                      :label="t('inference.chooseImage')"
                      prepend-icon="mdi-image"
                      :hint="t('inference.imageSizeHint')"
                      persistent-hint
                      variant="outlined"
                      @update:model-value="onImageSelected"
                    />
                    <v-btn
                      class="btn-primary-action run-btn"
                      block
                      color="primary"
                      :loading="imageLoading"
                      :disabled="
                        !selectedNodeId || !imageFile || !canSubmitImage
                      "
                      @click="submitImage"
                      >{{ t("inference.runImage") }}</v-btn
                    >

                    <v-row class="preview-grid mt-4">
                      <v-col cols="12" md="5">
                        <v-card
                          class="preview-box"
                          rounded="xl"
                          variant="elevated"
                        >
                          <v-card-title
                            class="preview-box-title text-subtitle-2 font-weight-bold"
                          >
                            {{ t("inference.chooseImage") }}
                          </v-card-title>
                          <v-card-text class="preview-box-content">
                            <v-img
                              v-if="imageInputPreviewUrl"
                              class="result-media"
                              :src="imageInputPreviewUrl"
                              cover
                              rounded="xl"
                            />
                            <div
                              v-else
                              class="preview-placeholder text-medium-emphasis"
                            >
                              {{ t("inference.imageRequired") }}
                            </div>
                          </v-card-text>
                        </v-card>
                      </v-col>

                      <v-col cols="12" md="2" class="preview-flow-wrap">
                        <div
                          class="preview-flow"
                          :class="{
                            'preview-flow--active': imageFlowHighlighted,
                          }"
                          aria-hidden="true"
                        >
                          <v-icon
                            class="preview-flow-icon preview-flow-icon--desktop"
                            >mdi-arrow-right-bold-circle-outline</v-icon
                          >
                          <v-icon
                            class="preview-flow-icon preview-flow-icon--mobile"
                            >mdi-arrow-down-bold-circle-outline</v-icon
                          >
                          <div class="preview-flow-text">
                            {{ t("common.apply") }}
                          </div>
                        </div>
                      </v-col>

                      <v-col cols="12" md="5">
                        <v-card
                          class="preview-box"
                          rounded="xl"
                          variant="elevated"
                        >
                          <v-card-title
                            class="preview-box-title text-subtitle-2 font-weight-bold"
                          >
                            {{ t("inference.resultSummary") }}
                          </v-card-title>
                          <v-card-text class="preview-box-content">
                            <v-img
                              v-if="
                                imageResult &&
                                pickMediaUrl(imageResult, [
                                  'resultUrl',
                                  'result_url',
                                  'imageUrl',
                                  'image_url',
                                  'resultImageUrl',
                                  'renderedImageUrl',
                                ])
                              "
                              class="result-media"
                              :src="
                                pickMediaUrl(imageResult, [
                                  'resultUrl',
                                  'result_url',
                                  'imageUrl',
                                  'image_url',
                                  'resultImageUrl',
                                  'renderedImageUrl',
                                ])
                              "
                              cover
                              rounded="xl"
                            />
                            <div
                              v-else
                              class="preview-placeholder text-medium-emphasis"
                            >
                              {{ t("inference.detailItemsEmpty") }}
                            </div>
                          </v-card-text>
                        </v-card>
                      </v-col>
                    </v-row>

                    <div v-if="imageResult" class="mt-3 result-zone">
                      <div class="result-status mt-1">
                        <v-chip
                          :color="statusColor(imageResult.status)"
                          size="small"
                          variant="flat"
                        >
                          {{ statusLabel(imageResult.status) }}
                        </v-chip>
                      </div>
                      <v-row class="mt-3 info-grid">
                        <v-col cols="12" md="6">
                          <v-card
                            class="result-panel"
                            rounded="lg"
                            variant="elevated"
                          >
                            <v-card-title class="d-flex align-center ga-2">
                              {{ t("inference.resultSummary") }}
                              <v-spacer />
                              <v-btn
                                color="primary"
                                size="small"
                                variant="text"
                                @click="copySummary(imageResult)"
                              >
                                {{ t("common.copy") }}
                              </v-btn>
                            </v-card-title>
                            <v-card-text class="result-panel-body">
                              <div
                                ref="imageSummaryScrollRef"
                                class="result-panel-scroll scroll-elegant scroll-edge-hint"
                                :class="{
                                  'scroll-at-top':
                                    scrollState.imageSummary.atTop,
                                  'scroll-at-bottom':
                                    scrollState.imageSummary.atBottom,
                                }"
                                @scroll.passive="
                                  handleScroll('imageSummary', $event)
                                "
                              >
                                <v-row>
                                  <v-col
                                    v-for="row in resultRows(imageResult)"
                                    :key="`image-${row.label}`"
                                    cols="12"
                                    md="6"
                                  >
                                    <div
                                      class="text-caption text-medium-emphasis"
                                    >
                                      {{ row.label }}
                                    </div>
                                    <div class="text-body-2">
                                      {{ row.value }}
                                    </div>
                                  </v-col>
                                </v-row>
                              </div>
                            </v-card-text>
                          </v-card>
                        </v-col>
                        <v-col cols="12" md="6">
                          <v-card
                            class="result-panel"
                            rounded="lg"
                            variant="elevated"
                          >
                            <v-card-title class="d-flex align-center ga-2">
                              {{ t("inference.detectionDetails") }}
                              <v-spacer />
                              <v-btn
                                v-if="detailRowCount(imageResult) > 12"
                                color="primary"
                                size="small"
                                variant="text"
                                @click="
                                  showAllImageDetails = !showAllImageDetails
                                "
                              >
                                {{
                                  showAllImageDetails
                                    ? t("common.showLess")
                                    : t("common.showAll")
                                }}
                              </v-btn>
                            </v-card-title>
                            <v-card-text class="result-panel-body">
                              <div
                                ref="imageDetailScrollRef"
                                class="result-panel-scroll scroll-elegant scroll-edge-hint"
                                :class="{
                                  'scroll-at-top':
                                    scrollState.imageDetail.atTop,
                                  'scroll-at-bottom':
                                    scrollState.imageDetail.atBottom,
                                }"
                                @scroll.passive="
                                  handleScroll('imageDetail', $event)
                                "
                              >
                                <v-row
                                  v-if="
                                    detailRowsByMode(
                                      imageResult,
                                      showAllImageDetails,
                                    ).length > 0
                                  "
                                >
                                  <v-col
                                    v-for="row in detailRowsByMode(
                                      imageResult,
                                      showAllImageDetails,
                                    )"
                                    :key="`image-detail-${row.key}`"
                                    cols="12"
                                    md="6"
                                  >
                                    <div
                                      class="text-caption text-medium-emphasis mono"
                                    >
                                      {{ row.key }}
                                    </div>
                                    <div class="text-body-2">
                                      {{ row.value }}
                                    </div>
                                  </v-col>
                                </v-row>
                                <div
                                  v-else
                                  class="text-body-2 text-medium-emphasis"
                                >
                                  {{ t("inference.detailItemsEmpty") }}
                                </div>
                              </div>
                            </v-card-text>
                          </v-card>
                        </v-col>
                      </v-row>
                    </div>
                  </v-col>
                </v-row>
              </v-card-text>
            </v-window-item>

            <v-window-item value="video">
              <v-card-text class="task-panel">
                <v-row class="task-layout" align="start">
                  <v-col cols="12" lg="3">
                    <InferenceNodeSelectorCard
                      v-model="selectedNodeId"
                      :nodes="nodes"
                    />
                  </v-col>

                  <v-col cols="12" lg="9">
                    <v-file-input
                      class="task-input"
                      :model-value="videoFile"
                      accept="video/*"
                      :label="t('inference.chooseVideo')"
                      prepend-icon="mdi-video"
                      :hint="t('inference.videoSizeHint')"
                      persistent-hint
                      variant="outlined"
                      @update:model-value="onVideoSelected"
                    />
                    <v-btn
                      class="btn-primary-action run-btn"
                      block
                      color="primary"
                      :loading="videoLoading"
                      :disabled="
                        !selectedNodeId || !videoFile || !canSubmitVideo
                      "
                      @click="submitVideo"
                      >{{ t("inference.runVideo") }}</v-btn
                    >

                    <v-row class="preview-grid mt-4">
                      <v-col cols="12" md="5">
                        <v-card
                          class="preview-box"
                          rounded="xl"
                          variant="elevated"
                        >
                          <v-card-title
                            class="preview-box-title text-subtitle-2 font-weight-bold"
                          >
                            {{ t("inference.chooseVideo") }}
                          </v-card-title>
                          <v-card-text class="preview-box-content">
                            <video
                              v-if="videoInputPreviewUrl"
                              class="result-media"
                              controls
                            >
                              <source :src="videoInputPreviewUrl" />
                            </video>
                            <div
                              v-else
                              class="preview-placeholder text-medium-emphasis"
                            >
                              {{ t("inference.videoRequired") }}
                            </div>
                          </v-card-text>
                        </v-card>
                      </v-col>

                      <v-col cols="12" md="2" class="preview-flow-wrap">
                        <div
                          class="preview-flow"
                          :class="{
                            'preview-flow--active': videoFlowHighlighted,
                          }"
                          aria-hidden="true"
                        >
                          <v-icon
                            class="preview-flow-icon preview-flow-icon--desktop"
                            >mdi-arrow-right-bold-circle-outline</v-icon
                          >
                          <v-icon
                            class="preview-flow-icon preview-flow-icon--mobile"
                            >mdi-arrow-down-bold-circle-outline</v-icon
                          >
                          <div class="preview-flow-text">
                            {{ t("common.apply") }}
                          </div>
                        </div>
                      </v-col>

                      <v-col cols="12" md="5">
                        <v-card
                          class="preview-box"
                          rounded="xl"
                          variant="elevated"
                        >
                          <v-card-title
                            class="preview-box-title text-subtitle-2 font-weight-bold"
                          >
                            {{ t("inference.resultSummary") }}
                          </v-card-title>
                          <v-card-text class="preview-box-content">
                            <video
                              v-if="
                                videoResult &&
                                pickMediaUrl(videoResult, [
                                  'resultUrl',
                                  'result_url',
                                  'videoUrl',
                                  'video_url',
                                  'resultVideoUrl',
                                  'renderedVideoUrl',
                                ])
                              "
                              class="result-media"
                              controls
                            >
                              <source
                                :src="
                                  pickMediaUrl(videoResult, [
                                    'resultUrl',
                                    'result_url',
                                    'videoUrl',
                                    'video_url',
                                    'resultVideoUrl',
                                    'renderedVideoUrl',
                                  ])
                                "
                              />
                            </video>
                            <div
                              v-else
                              class="preview-placeholder text-medium-emphasis"
                            >
                              {{ t("inference.detailItemsEmpty") }}
                            </div>
                          </v-card-text>
                        </v-card>
                      </v-col>
                    </v-row>

                    <div v-if="videoResult" class="mt-3 result-zone">
                      <div class="result-status mt-1">
                        <v-chip
                          :color="statusColor(videoResult.status)"
                          size="small"
                          variant="flat"
                        >
                          {{ statusLabel(videoResult.status) }}
                        </v-chip>
                      </div>
                      <v-row class="mt-3 info-grid">
                        <v-col cols="12" md="6">
                          <v-card
                            class="result-panel"
                            rounded="lg"
                            variant="elevated"
                          >
                            <v-card-title class="d-flex align-center ga-2">
                              {{ t("inference.resultSummary") }}
                              <v-spacer />
                              <v-btn
                                color="primary"
                                size="small"
                                variant="text"
                                @click="copySummary(videoResult)"
                              >
                                {{ t("common.copy") }}
                              </v-btn>
                            </v-card-title>
                            <v-card-text class="result-panel-body">
                              <div
                                ref="videoSummaryScrollRef"
                                class="result-panel-scroll scroll-elegant scroll-edge-hint"
                                :class="{
                                  'scroll-at-top':
                                    scrollState.videoSummary.atTop,
                                  'scroll-at-bottom':
                                    scrollState.videoSummary.atBottom,
                                }"
                                @scroll.passive="
                                  handleScroll('videoSummary', $event)
                                "
                              >
                                <v-row>
                                  <v-col
                                    v-for="row in resultRows(videoResult)"
                                    :key="`video-${row.label}`"
                                    cols="12"
                                    md="6"
                                  >
                                    <div
                                      class="text-caption text-medium-emphasis"
                                    >
                                      {{ row.label }}
                                    </div>
                                    <div class="text-body-2">
                                      {{ row.value }}
                                    </div>
                                  </v-col>
                                </v-row>
                              </div>
                            </v-card-text>
                          </v-card>
                        </v-col>
                        <v-col cols="12" md="6">
                          <v-card
                            class="result-panel"
                            rounded="lg"
                            variant="elevated"
                          >
                            <v-card-title class="d-flex align-center ga-2">
                              {{ t("inference.detectionDetails") }}
                              <v-spacer />
                              <v-btn
                                v-if="detailRowCount(videoResult) > 12"
                                color="primary"
                                size="small"
                                variant="text"
                                @click="
                                  showAllVideoDetails = !showAllVideoDetails
                                "
                              >
                                {{
                                  showAllVideoDetails
                                    ? t("common.showLess")
                                    : t("common.showAll")
                                }}
                              </v-btn>
                            </v-card-title>
                            <v-card-text class="result-panel-body">
                              <div
                                ref="videoDetailScrollRef"
                                class="result-panel-scroll scroll-elegant scroll-edge-hint"
                                :class="{
                                  'scroll-at-top':
                                    scrollState.videoDetail.atTop,
                                  'scroll-at-bottom':
                                    scrollState.videoDetail.atBottom,
                                }"
                                @scroll.passive="
                                  handleScroll('videoDetail', $event)
                                "
                              >
                                <v-row
                                  v-if="
                                    detailRowsByMode(
                                      videoResult,
                                      showAllVideoDetails,
                                    ).length > 0
                                  "
                                >
                                  <v-col
                                    v-for="row in detailRowsByMode(
                                      videoResult,
                                      showAllVideoDetails,
                                    )"
                                    :key="`video-detail-${row.key}`"
                                    cols="12"
                                    md="6"
                                  >
                                    <div
                                      class="text-caption text-medium-emphasis mono"
                                    >
                                      {{ row.key }}
                                    </div>
                                    <div class="text-body-2">
                                      {{ row.value }}
                                    </div>
                                  </v-col>
                                </v-row>
                                <div
                                  v-else
                                  class="text-body-2 text-medium-emphasis"
                                >
                                  {{ t("inference.detailItemsEmpty") }}
                                </div>
                              </div>
                            </v-card-text>
                          </v-card>
                        </v-col>
                      </v-row>
                    </div>
                  </v-col>
                </v-row>
              </v-card-text>
            </v-window-item>
          </v-window>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<style scoped>
.inference-page {
  gap: 22px;
}

.task-card-col {
  display: flex;
}

.task-card {
  width: 100%;
  border: 1px solid rgba(var(--uav-primary-rgb), 0.22);
  box-shadow: 0 18px 34px rgba(var(--uav-primary-rgb), 0.14);
}

.task-layout {
  row-gap: 12px;
}

.preview-grid {
  row-gap: 14px;
}

.preview-flow-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-flow {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: rgba(var(--uav-primary-rgb), 0.72);
  transition:
    color 0.26s ease,
    transform 0.26s ease,
    filter 0.26s ease;
}

.preview-flow--active {
  color: rgba(var(--uav-primary-rgb), 0.98);
  transform: scale(1.08);
  filter: drop-shadow(0 6px 14px rgba(var(--uav-primary-rgb), 0.34));
}

.preview-flow-icon {
  font-size: 34px;
}

.preview-flow-icon--mobile {
  display: none;
}

.preview-flow-text {
  font-size: 0.78rem;
  letter-spacing: 0.03em;
  text-transform: uppercase;
}

.preview-box {
  min-height: 420px;
  border: 1px solid rgba(var(--uav-primary-rgb), 0.24);
  background: linear-gradient(
    165deg,
    rgba(255, 255, 255, 0.82),
    rgba(241, 249, 255, 0.74)
  );
  box-shadow: 0 14px 28px rgba(var(--uav-primary-rgb), 0.12);
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    border-color 0.22s ease;
}

.preview-box:hover {
  transform: translateY(-4px);
  border-color: rgba(var(--uav-primary-rgb), 0.34);
  box-shadow: 0 20px 34px rgba(var(--uav-primary-rgb), 0.18);
}

.preview-box-title {
  border-bottom: 1px solid rgba(var(--uav-primary-rgb), 0.16);
  color: rgba(12, 56, 82, 0.94);
}

.preview-box-content {
  padding-top: 18px;
}

.preview-placeholder {
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed rgba(var(--uav-primary-rgb), 0.28);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.38);
}

.result-media {
  width: 100%;
  min-height: 320px;
  max-height: 420px;
  aspect-ratio: 16 / 9;
  border-radius: 16px;
  border: 1px solid rgba(var(--uav-primary-rgb), 0.18);
  background: rgba(0, 0, 0, 0.05);
}

.task-input {
  margin-bottom: 4px;
}

.run-btn {
  margin-top: 8px;
}

.result-zone {
  border-top: 1px dashed rgba(var(--uav-primary-rgb), 0.22);
  padding-top: 14px;
}

.info-grid {
  row-gap: 12px;
}

.result-panel {
  height: 100%;
  min-height: 420px;
  max-height: 420px;
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(var(--uav-primary-rgb), 0.22);
  box-shadow: 0 10px 22px rgba(var(--uav-primary-rgb), 0.12);
}

.result-panel :deep(.v-card-title) {
  border-bottom: 1px solid rgba(var(--uav-primary-rgb), 0.14);
  background: rgba(236, 248, 255, 0.62);
}

.result-panel-body {
  flex: 1;
  min-height: 0;
  padding-top: 14px;
}

.result-panel-scroll {
  height: 100%;
  min-height: 0;
  overflow: auto;
  padding-right: 6px;
}

.result-status {
  display: flex;
  justify-content: flex-start;
}

.task-tabs {
  border-bottom: 1px solid rgba(var(--uav-primary-rgb), 0.2);
}

.task-panel {
  padding: 24px 28px 28px;
}

@media (max-width: 960px) {
  .task-panel {
    padding: 20px 18px 22px;
  }

  .preview-box {
    min-height: 360px;
  }

  .preview-placeholder {
    min-height: 260px;
  }

  .result-media {
    min-height: 240px;
    max-height: 320px;
  }

  .result-panel {
    min-height: auto;
    max-height: none;
  }

  .result-panel-scroll {
    max-height: none;
    padding-right: 2px;
  }

  .preview-flow-icon--desktop {
    display: none;
  }

  .preview-flow-icon--mobile {
    display: inline-flex;
  }
}
</style>
