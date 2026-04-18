<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import {
  applyTemplate,
  changeActiveWeight,
  createNodeParamTemplate,
  createNode,
  deleteNodeParamTemplate,
  deleteWeight,
  fetchNodeDetail,
  fetchNodePage,
  fetchNodeParamTemplates,
  fetchNodeWeightSummary,
  removeNode,
  syncNode,
  updateNodeParamTemplate,
  updateNode,
  uploadWeight,
} from "@/api";
import { useScrollEdgeState } from "@/composables/useScrollEdgeState";
import type {
  YoloNode,
  YoloNodeParamCreateRequest,
  YoloNodeParamTemplate,
  YoloNodeParamUpdateRequest,
  YoloNodeUpsertRequest,
  YoloNodeWeightsSummary,
} from "@/types";
import { notify } from "@/composables/useNotifier";
import ConfirmActionDialog from "@/components/ConfirmActionDialog.vue";
import PromptActionDialog from "@/components/PromptActionDialog.vue";
import PageHero from "@/components/PageHero.vue";

const { t } = useI18n();
const route = useRoute();

const loading = ref(false);
const saving = ref(false);

const nodes = ref<YoloNode[]>([]);
const total = ref(0);
const current = ref(1);
const size = ref(10);

const filters = reactive({
  nodeName: "",
  status: null as number | null,
});

const dialog = ref(false);
const editId = ref<number | null>(null);

const form = reactive<YoloNodeUpsertRequest>({
  nodeName: "",
  host: "",
  port: "",
  description: "",
  httpProtocol: "http",
  apiVersion: "v1",
});

const nodeFormRef = ref<{
  validate: () => Promise<{ valid: boolean }>;
} | null>(null);

const detailDialog = ref(false);
const detailLoading = ref(false);
const detail = ref<Record<string, unknown> | null>(null);
const showAllNodeDetails = ref(false);
const selectedNode = ref<YoloNode | null>(null);
const workspaceNodeId = ref<number | null>(null);
const workspaceLoading = ref(false);
const nodeTemplates = ref<YoloNodeParamTemplate[]>([]);
const nodeWeights = ref<YoloNodeWeightsSummary | null>(null);
const uploadingWeightNodeId = ref<number | null>(null);
const dialogBusy = ref(false);
const nodeDetailScrollRef = ref<HTMLElement | null>(null);

const templateDialog = ref(false);
const templateDialogMode = ref<"create" | "edit">("create");
const templateDialogOriginalName = ref("");
const templateForm = reactive({
  templateName: "",
  description: "",
  paramsJson: "{}",
  isActive: false,
});
const templateFormError = ref("");

const { scrollState, refreshByElement, handleScroll } = useScrollEdgeState(
  ["nodeDetail"] as const,
  { threshold: 3 },
);

const confirmDialog = reactive({
  show: false,
  title: "",
  message: "",
  nodeId: null as number | null,
});

const promptDialog = reactive({
  show: false,
  title: "",
  label: "",
  value: "",
  nodeId: null as number | null,
  action: "template" as "template" | "switch" | "deleteWeight",
});

const WEIGHT_MAX_SIZE_BYTES = 1024 * 1024 * 1024;
const WEIGHT_ALLOWED_EXTENSIONS = [".pt", ".onnx"];

function getFileExtension(filename: string): string {
  const index = filename.lastIndexOf(".");
  if (index < 0) {
    return "";
  }
  return filename.slice(index).toLowerCase();
}

function canUploadWeight(file: File): boolean {
  const extension = getFileExtension(file.name);
  if (!WEIGHT_ALLOWED_EXTENSIONS.includes(extension)) {
    notify(t("node.weightTypeInvalid"), "warning");
    return false;
  }

  if (file.size > WEIGHT_MAX_SIZE_BYTES) {
    notify(t("node.weightTooLarge"), "warning");
    return false;
  }

  return true;
}

const pageCount = computed(() =>
  Math.max(1, Math.ceil(total.value / size.value)),
);

const onlineCountInPage = computed(
  () =>
    nodes.value.filter((item) => normalizeNodeStatus(item.status) === 1).length,
);

const offlineCountInPage = computed(
  () =>
    nodes.value.filter((item) => normalizeNodeStatus(item.status) === 0).length,
);

const exceptionCountInPage = computed(
  () =>
    nodes.value.filter((item) => normalizeNodeStatus(item.status) === 2).length,
);

const workspaceNode = computed(
  () => nodes.value.find((item) => item.id === workspaceNodeId.value) ?? null,
);

const workspaceTitle = computed(() =>
  workspaceNode.value
    ? `${workspaceNode.value.nodeName ?? t("node.detail")}`
    : t("node.workspaceEmpty"),
);

const paramTemplateCount = computed(() => nodeTemplates.value.length);

const activeTemplateName = computed(() => {
  const active = nodeTemplates.value.find((item) => Boolean(item.isActive));
  return active?.templateName ? String(active.templateName) : "";
});

const weightCount = computed(
  () => nodeWeights.value?.availableWeights.length ?? 0,
);

const headers = computed(() => [
  { title: t("node.table.id"), key: "id", width: 90 },
  { title: t("node.table.nodeName"), key: "nodeName" },
  { title: t("node.table.host"), key: "host" },
  { title: t("node.table.port"), key: "port", width: 100 },
  { title: t("node.table.status"), key: "status", width: 110 },
  { title: t("node.table.activeWeight"), key: "activeWeightName" },
  {
    title: t("node.table.actions"),
    key: "actions",
    sortable: false,
    width: 280,
  },
]);

const statusItems = computed(() => [
  { title: t("node.all"), value: null },
  { title: t("node.offline"), value: 0 },
  { title: t("node.online"), value: 1 },
  { title: t("node.exception"), value: 2 },
]);

const nodeNameRules = computed(() => [
  (value: string) => !!value?.trim() || t("node.nodeNameRequired"),
]);

const hostRules = computed(() => [
  (value: string) => !!value?.trim() || t("node.hostRequired"),
]);

const portRules = computed(() => [
  (value: string) => !!value?.trim() || t("node.portRequired"),
  (value: string) => {
    const port = Number(value);
    return Number.isInteger(port) && port >= 1 && port <= 65535
      ? true
      : t("node.portInvalid");
  },
]);

const protocolRules = computed(() => [
  (value: string) => {
    const protocol = String(value || "").toLowerCase();
    return protocol === "http" || protocol === "https"
      ? true
      : t("node.protocolInvalid");
  },
]);

function resetForm(): void {
  form.nodeName = "";
  form.host = "";
  form.port = "";
  form.description = "";
  form.httpProtocol = "http";
  form.apiVersion = "v1";
  editId.value = null;
}

function fillForm(node: YoloNode): void {
  form.nodeName = String(node.nodeName ?? "");
  form.host = String(node.host ?? "");
  form.port = String(node.port ?? "");
  form.description = String(node.description ?? "");
  form.httpProtocol = String(node.httpProtocol ?? "http");
  form.apiVersion = String(node.apiVersion ?? "v1");
}

async function loadNodes(): Promise<void> {
  loading.value = true;
  try {
    const data = await fetchNodePage({
      current: current.value,
      size: size.value,
      nodeName: filters.nodeName.trim() || undefined,
      status: filters.status,
    });

    nodes.value = data.records ?? [];
    total.value = Number(data.total ?? 0);

    if (
      nodes.value.length > 0 &&
      !nodes.value.some((item) => item.id === workspaceNodeId.value)
    ) {
      workspaceNodeId.value = Number(nodes.value[0].id);
    }
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("node.loadFailed"),
      "error",
    );
  } finally {
    loading.value = false;
  }
}

async function openCreate(): Promise<void> {
  resetForm();
  dialog.value = true;
}

async function openEdit(node: YoloNode): Promise<void> {
  resetForm();
  editId.value = Number(node.id);
  fillForm(node);
  dialog.value = true;
}

async function submitNode(): Promise<void> {
  if (saving.value) {
    return;
  }

  const result = await nodeFormRef.value?.validate();
  if (!result?.valid) {
    return;
  }

  saving.value = true;
  try {
    if (editId.value) {
      await updateNode(editId.value, { ...form });
      notify(t("node.updated"), "success");
    } else {
      await createNode({ ...form });
      notify(t("node.created"), "success");
    }

    dialog.value = false;
    await loadNodes();
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("node.saveFailed"),
      "error",
    );
  } finally {
    saving.value = false;
  }
}

async function remove(nodeId: number): Promise<void> {
  confirmDialog.title = t("node.deleteTitle");
  confirmDialog.message = t("node.deleteConfirm");
  confirmDialog.nodeId = nodeId;
  confirmDialog.show = true;
}

async function confirmRemoveNode(): Promise<void> {
  if (!confirmDialog.nodeId || dialogBusy.value) {
    return;
  }

  try {
    dialogBusy.value = true;
    await removeNode(confirmDialog.nodeId);
    notify(t("node.deleteSuccess"), "success");
    confirmDialog.show = false;
    await loadNodes();
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("node.deleteFailed"),
      "error",
    );
  } finally {
    dialogBusy.value = false;
  }
}

async function openDetail(node: YoloNode): Promise<void> {
  detailDialog.value = true;
  detailLoading.value = true;
  selectedNode.value = node;
  workspaceNodeId.value = Number(node.id);
  try {
    detail.value = await fetchNodeDetail(Number(node.id));
    showAllNodeDetails.value = false;
  } catch (error) {
    detail.value = null;
    notify(
      error instanceof Error ? error.message : t("node.detailFailed"),
      "error",
    );
  } finally {
    detailLoading.value = false;
  }
}

async function loadWorkspace(nodeId: number | null): Promise<void> {
  if (!nodeId) {
    nodeTemplates.value = [];
    nodeWeights.value = null;
    return;
  }

  workspaceLoading.value = true;
  try {
    const [templates, weights] = await Promise.all([
      fetchNodeParamTemplates(nodeId),
      fetchNodeWeightSummary(nodeId),
    ]);
    nodeTemplates.value = templates ?? [];
    nodeWeights.value = weights ?? null;
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("node.loadFailed"),
      "error",
    );
  } finally {
    workspaceLoading.value = false;
  }
}

function openCreateTemplate(): void {
  templateDialogMode.value = "create";
  templateDialogOriginalName.value = "";
  templateForm.templateName = "";
  templateForm.description = "";
  templateForm.paramsJson = "{}";
  templateForm.isActive = false;
  templateFormError.value = "";
  templateDialog.value = true;
}

function openEditTemplate(template: YoloNodeParamTemplate): void {
  templateDialogMode.value = "edit";
  templateDialogOriginalName.value = String(template.templateName ?? "");
  templateForm.templateName = String(template.templateName ?? "");
  templateForm.description = String(template.description ?? "");
  templateForm.paramsJson = JSON.stringify(template.params ?? {}, null, 2);
  templateForm.isActive = Boolean(template.isActive);
  templateFormError.value = "";
  templateDialog.value = true;
}

async function submitTemplateForm(): Promise<void> {
  const nodeId = workspaceNodeId.value;
  if (!nodeId || dialogBusy.value) {
    return;
  }

  templateFormError.value = "";
  let params: Record<string, unknown>;
  try {
    params = JSON.parse(templateForm.paramsJson || "{}");
  } catch {
    templateFormError.value = t("node.templateJsonInvalid");
    return;
  }

  const payload: YoloNodeParamCreateRequest | YoloNodeParamUpdateRequest = {
    description: templateForm.description.trim() || undefined,
    params,
    isActive: templateForm.isActive,
  };

  try {
    dialogBusy.value = true;
    if (templateDialogMode.value === "create") {
      await createNodeParamTemplate(nodeId, {
        templateName: templateForm.templateName.trim(),
        description: templateForm.description.trim() || undefined,
        params,
        isActive: templateForm.isActive,
      });
      notify(t("node.templateCreated"), "success");
    } else {
      await updateNodeParamTemplate(
        nodeId,
        templateDialogOriginalName.value,
        payload,
      );
      notify(t("node.templateUpdated"), "success");
    }

    templateDialog.value = false;
    await loadWorkspace(nodeId);
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("node.templateFailed"),
      "error",
    );
  } finally {
    dialogBusy.value = false;
  }
}

async function removeTemplate(templateName: string): Promise<void> {
  const nodeId = workspaceNodeId.value;
  if (!nodeId) {
    return;
  }

  const confirmed = window.confirm(t("node.templateDeleteConfirm"));
  if (!confirmed) {
    return;
  }

  try {
    dialogBusy.value = true;
    await deleteNodeParamTemplate(nodeId, templateName);
    notify(t("node.templateDeleted"), "success");
    await loadWorkspace(nodeId);
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("node.templateFailed"),
      "error",
    );
  } finally {
    dialogBusy.value = false;
  }
}

async function applyWorkspaceTemplate(templateName: string): Promise<void> {
  const nodeId = workspaceNodeId.value;
  if (!nodeId) {
    return;
  }

  try {
    dialogBusy.value = true;
    await applyTemplate(nodeId, templateName);
    notify(t("node.templateSuccess"), "success");
    await loadNodes();
    await loadWorkspace(nodeId);
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("node.templateFailed"),
      "error",
    );
  } finally {
    dialogBusy.value = false;
  }
}

async function switchWorkspaceWeight(filename: string): Promise<void> {
  const nodeId = workspaceNodeId.value;
  if (!nodeId || dialogBusy.value) {
    return;
  }

  try {
    dialogBusy.value = true;
    await changeActiveWeight(nodeId, filename);
    notify(t("node.switchSuccess"), "success");
    await loadNodes();
    await loadWorkspace(nodeId);
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("node.switchFailed"),
      "error",
    );
  } finally {
    dialogBusy.value = false;
  }
}

async function deleteWorkspaceWeight(filename: string): Promise<void> {
  const nodeId = workspaceNodeId.value;
  if (!nodeId || dialogBusy.value) {
    return;
  }

  const confirmed = window.confirm(t("node.deleteWeightConfirm"));
  if (!confirmed) {
    return;
  }

  try {
    dialogBusy.value = true;
    await deleteWeight(nodeId, filename);
    notify(t("node.deleteWeightSuccess"), "success");
    await loadNodes();
    await loadWorkspace(nodeId);
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("node.deleteWeightFailed"),
      "error",
    );
  } finally {
    dialogBusy.value = false;
  }
}

async function triggerSync(nodeId: number): Promise<void> {
  try {
    await syncNode(nodeId);
    notify(t("node.syncSuccess"), "success");
    await loadNodes();
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("node.syncFailed"),
      "error",
    );
  }
}

async function triggerTemplate(nodeId: number): Promise<void> {
  promptDialog.title = t("node.templateTitle");
  promptDialog.label = t("node.templateLabel");
  promptDialog.value = "";
  promptDialog.nodeId = nodeId;
  promptDialog.action = "template";
  promptDialog.show = true;
}

async function submitPrompt(value: string): Promise<void> {
  if (!promptDialog.nodeId || dialogBusy.value) {
    return;
  }

  const nodeId = promptDialog.nodeId;
  try {
    dialogBusy.value = true;
    if (promptDialog.action === "template") {
      await applyTemplate(nodeId, value);
      notify(t("node.templateSuccess"), "success");
    } else if (promptDialog.action === "switch") {
      await changeActiveWeight(nodeId, value);
      notify(t("node.switchSuccess"), "success");
    } else {
      await deleteWeight(nodeId, value);
      notify(t("node.deleteWeightSuccess"), "success");
    }

    promptDialog.show = false;
    await loadNodes();
    if (selectedNode.value?.id === nodeId) {
      await openDetail(selectedNode.value);
    }
  } catch (error) {
    const fallback =
      promptDialog.action === "template"
        ? t("node.templateFailed")
        : promptDialog.action === "switch"
          ? t("node.switchFailed")
          : t("node.deleteWeightFailed");
    notify(error instanceof Error ? error.message : fallback, "error");
  } finally {
    dialogBusy.value = false;
  }
}

async function triggerWeightUpload(nodeId: number): Promise<void> {
  if (uploadingWeightNodeId.value !== null) {
    return;
  }

  const input = document.createElement("input");
  input.type = "file";
  input.accept = ".pt,.onnx";
  input.onchange = async () => {
    const target = input.files?.[0];
    if (!target) {
      notify(t("node.weightRequired"), "warning");
      return;
    }

    if (!canUploadWeight(target)) {
      return;
    }

    try {
      uploadingWeightNodeId.value = nodeId;
      await uploadWeight(nodeId, target);
      notify(t("node.uploadSuccess"), "success");
      await loadNodes();
      if (selectedNode.value?.id === nodeId) {
        await openDetail(selectedNode.value);
      }
    } catch (error) {
      notify(
        error instanceof Error ? error.message : t("node.uploadFailed"),
        "error",
      );
    } finally {
      uploadingWeightNodeId.value = null;
    }
  };
  input.click();
}

async function triggerChangeWeight(nodeId: number): Promise<void> {
  promptDialog.title = t("node.switchTitle");
  promptDialog.label = t("node.switchLabel");
  promptDialog.value = "";
  promptDialog.nodeId = nodeId;
  promptDialog.action = "switch";
  promptDialog.show = true;
}

async function triggerDeleteWeight(nodeId: number): Promise<void> {
  promptDialog.title = t("node.deleteWeightTitle");
  promptDialog.label = t("node.deleteWeightLabel");
  promptDialog.value = "";
  promptDialog.nodeId = nodeId;
  promptDialog.action = "deleteWeight";
  promptDialog.show = true;
}

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

function statusChipColor(status: unknown): string {
  if (normalizeNodeStatus(status) === 1) {
    return "success";
  }

  if (normalizeNodeStatus(status) === 2) {
    return "warning";
  }

  return "error";
}

function statusLabel(status: unknown): string {
  if (normalizeNodeStatus(status) === 1) {
    return t("node.state.online");
  }

  if (normalizeNodeStatus(status) === 2) {
    return t("node.state.exception");
  }

  return t("node.state.offline");
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

function detailRowCount(detailData: Record<string, unknown>): number {
  return Object.keys(detailData).length;
}

function detailRowsByMode(
  detailData: Record<string, unknown>,
  showAll: boolean,
): Array<{ key: string; value: string }> {
  const entries = Object.entries(detailData);
  const target = showAll ? entries : entries.slice(0, 16);
  return target.map(([key, value]) => ({ key, value: asText(value) }));
}

function detailSummaryText(detailData: Record<string, unknown>): string {
  const rows = [
    `${t("node.table.id")}: ${asText(detailData.id)}`,
    `${t("node.table.nodeName")}: ${asText(detailData.nodeName)}`,
    `${t("node.table.host")}: ${asText(detailData.host)}`,
    `${t("node.table.port")}: ${asText(detailData.port)}`,
    `${t("node.table.status")}: ${asText(detailData.status)}`,
    `${t("node.table.activeWeight")}: ${asText(detailData.activeWeightName)}`,
  ];
  return rows.join("\n");
}

async function copyNodeSummary(
  detailData: Record<string, unknown>,
): Promise<void> {
  try {
    await navigator.clipboard.writeText(detailSummaryText(detailData));
    notify(t("node.copiedSummary"), "success");
  } catch {
    notify(t("node.copySummaryFailed"), "error");
  }
}

function refreshNodeDetailScrollState(): void {
  refreshByElement("nodeDetail", nodeDetailScrollRef.value);
}

function parseStatusQuery(value: unknown): number | null {
  if (typeof value !== "string") {
    return null;
  }

  const numeric = Number(value);
  if (numeric === 0 || numeric === 1 || numeric === 2) {
    return numeric;
  }

  return null;
}

watch(
  () => route.query.status,
  async (statusQuery) => {
    const nextStatus = parseStatusQuery(statusQuery);
    if (filters.status !== nextStatus) {
      filters.status = nextStatus;
      current.value = 1;
    }
    await loadNodes();
  },
  { immediate: true },
);

watch(
  workspaceNodeId,
  async (nodeId) => {
    await loadWorkspace(nodeId);
  },
  { immediate: true },
);

watch(
  () => [
    detailDialog.value,
    detailLoading.value,
    detail.value,
    showAllNodeDetails.value,
  ],
  async () => {
    await nextTick();
    refreshNodeDetailScrollState();
  },
  { flush: "post" },
);
</script>

<template>
  <div class="page-shell node-page">
    <PageHero
      :title="t('node.title')"
      :subtitle="`${t('node.table.nodeName')} · ${t('node.table.activeWeight')}`"
    >
      <template #actions>
        <v-btn
          class="btn-primary-action"
          color="primary"
          prepend-icon="mdi-plus"
          variant="elevated"
          @click="openCreate"
          >{{ t("node.addNode") }}</v-btn
        >
      </template>
    </PageHero>

    <v-card class="card-ambient card-spacious" rounded="xl">
      <v-card-title class="text-subtitle-1 font-weight-bold">
        {{ t("common.apply") }} / {{ t("common.reset") }}
      </v-card-title>
      <v-card-text>
        <v-row class="filter-row">
          <v-col cols="12" md="4">
            <v-text-field
              v-model="filters.nodeName"
              clearable
              :label="t('node.nodeName')"
              prepend-inner-icon="mdi-magnify"
              variant="outlined"
            />
          </v-col>
          <v-col cols="12" md="3">
            <v-select
              v-model="filters.status"
              :items="statusItems"
              item-title="title"
              item-value="value"
              :label="t('common.status')"
              variant="outlined"
            />
          </v-col>
          <v-col
            cols="12"
            md="5"
            class="d-flex ga-2 align-end justify-md-end flex-wrap"
          >
            <v-btn
              class="btn-primary-action"
              color="primary"
              prepend-icon="mdi-filter"
              @click="
                current = 1;
                loadNodes();
              "
              >{{ t("common.apply") }}</v-btn
            >
            <v-btn
              class="btn-secondary-action"
              variant="tonal"
              @click="
                filters.nodeName = '';
                filters.status = null;
                current = 1;
                loadNodes();
              "
              >{{ t("common.reset") }}</v-btn
            >
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <v-row>
      <v-col cols="12" md="4">
        <v-card class="card-ambient card-spacious summary-card" rounded="xl">
          <v-card-title class="text-subtitle-1 font-weight-bold">{{
            t("node.table.status")
          }}</v-card-title>
          <v-card-text>
            <div class="status-chip-wall">
              <v-chip color="success" variant="flat"
                >{{ t("node.online") }} · {{ onlineCountInPage }}</v-chip
              >
              <v-chip color="error" variant="flat"
                >{{ t("node.offline") }} · {{ offlineCountInPage }}</v-chip
              >
              <v-chip color="warning" variant="flat"
                >{{ t("node.exception") }} · {{ exceptionCountInPage }}</v-chip
              >
            </div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="8">
        <v-card class="card-ambient card-spacious summary-card" rounded="xl">
          <v-card-title class="d-flex align-center ga-3 flex-wrap">
            <span class="text-subtitle-1 font-weight-bold">
              {{ t("node.workspaceTitle") }}
            </span>
            <v-spacer />
            <v-chip color="primary" variant="tonal">
              {{ workspaceTitle }}
            </v-chip>
          </v-card-title>
          <v-card-text>
            <div class="workspace-toolbar">
              <v-select
                v-model="workspaceNodeId"
                :disabled="nodes.length === 0"
                :items="nodes"
                :label="t('node.workspaceNode')"
                item-title="nodeName"
                item-value="id"
                variant="outlined"
                class="workspace-node-select"
              />
              <div class="workspace-actions">
                <v-btn
                  class="btn-secondary-action"
                  color="primary"
                  prepend-icon="mdi-refresh"
                  variant="tonal"
                  :loading="workspaceLoading"
                  @click="loadWorkspace(workspaceNodeId)"
                >
                  {{ t("common.refresh") }}
                </v-btn>
                <v-btn
                  class="btn-primary-action"
                  color="primary"
                  prepend-icon="mdi-tune-variant"
                  variant="elevated"
                  :disabled="!workspaceNodeId"
                  @click="openCreateTemplate"
                >
                  {{ t("node.templateCreate") }}
                </v-btn>
                <v-btn
                  class="btn-secondary-action"
                  color="secondary"
                  prepend-icon="mdi-upload"
                  variant="tonal"
                  :disabled="!workspaceNodeId"
                  @click="triggerWeightUpload(Number(workspaceNodeId))"
                >
                  {{ t("node.uploadWeight") }}
                </v-btn>
              </div>
            </div>
            <v-row class="mt-4" dense>
              <v-col cols="12" md="6">
                <v-card variant="tonal" rounded="lg" class="h-100">
                  <v-card-title class="d-flex align-center ga-2">
                    <span class="text-subtitle-2 font-weight-bold">
                      {{ t("node.paramTemplates") }}
                    </span>
                    <v-spacer />
                    <v-chip size="small" variant="flat" color="primary">
                      {{ paramTemplateCount }}
                    </v-chip>
                  </v-card-title>
                  <v-card-text>
                    <v-chip
                      v-if="activeTemplateName"
                      class="mb-3"
                      color="success"
                      variant="flat"
                    >
                      {{ t("node.activeTemplate") }}:
                      {{ activeTemplateName }}
                    </v-chip>
                    <div
                      v-if="nodeTemplates.length > 0"
                      class="workspace-list scroll-elegant"
                    >
                      <v-card
                        v-for="template in nodeTemplates"
                        :key="template.templateName"
                        rounded="lg"
                        variant="outlined"
                        class="workspace-item"
                      >
                        <v-card-title class="d-flex align-center ga-2">
                          <span
                            class="text-body-1 font-weight-medium"
                            :class="{
                              'workspace-template-name--active':
                                template.isActive,
                            }"
                          >
                            {{ template.templateName }}
                          </span>
                          <v-spacer />
                          <v-tooltip
                            :text="t('node.templateTitle')"
                            location="top"
                          >
                            <template #activator="{ props }">
                              <v-btn
                                icon="mdi-play"
                                size="x-small"
                                variant="text"
                                class="workspace-icon-btn"
                                :color="
                                  template.isActive ? 'success' : undefined
                                "
                                :disabled="dialogBusy"
                                v-bind="props"
                                @click="
                                  applyWorkspaceTemplate(template.templateName)
                                "
                              />
                            </template>
                          </v-tooltip>
                          <v-tooltip
                            :text="t('node.templateEdit')"
                            location="top"
                          >
                            <template #activator="{ props }">
                              <v-btn
                                icon="mdi-pencil"
                                size="x-small"
                                variant="text"
                                class="workspace-icon-btn"
                                :disabled="dialogBusy"
                                v-bind="props"
                                @click="openEditTemplate(template)"
                              />
                            </template>
                          </v-tooltip>
                          <v-tooltip :text="t('common.delete')" location="top">
                            <template #activator="{ props }">
                              <v-btn
                                icon="mdi-delete"
                                size="x-small"
                                variant="text"
                                class="workspace-icon-btn workspace-icon-btn--danger"
                                :disabled="dialogBusy"
                                v-bind="props"
                                @click="removeTemplate(template.templateName)"
                              />
                            </template>
                          </v-tooltip>
                        </v-card-title>
                        <v-card-subtitle v-if="template.description">
                          {{ template.description }}
                        </v-card-subtitle>
                        <v-card-text>
                          <pre class="workspace-json">{{
                            asText(template.params)
                          }}</pre>
                        </v-card-text>
                      </v-card>
                    </div>
                    <div v-else class="text-body-2 text-medium-emphasis">
                      {{ t("node.templateEmpty") }}
                    </div>
                  </v-card-text>
                </v-card>
              </v-col>
              <v-col cols="12" md="6">
                <v-card variant="tonal" rounded="lg" class="h-100">
                  <v-card-title class="d-flex align-center ga-2">
                    <span class="text-subtitle-2 font-weight-bold">
                      {{ t("node.weightSummary") }}
                    </span>
                    <v-spacer />
                    <v-chip size="small" variant="flat" color="secondary">
                      {{ weightCount }}
                    </v-chip>
                  </v-card-title>
                  <v-card-text>
                    <v-chip
                      v-if="nodeWeights?.activeWeight"
                      class="mb-3"
                      color="success"
                      variant="flat"
                    >
                      {{ t("node.activeWeight") }}:
                      {{ nodeWeights.activeWeight }}
                    </v-chip>
                    <div
                      v-if="nodeWeights?.availableWeights?.length"
                      class="workspace-weight-wall scroll-elegant"
                    >
                      <div
                        v-for="filename in nodeWeights.availableWeights"
                        :key="filename"
                        class="workspace-weight-item"
                      >
                        <v-chip
                          :color="
                            filename === nodeWeights?.activeWeight
                              ? 'success'
                              : 'default'
                          "
                          variant="tonal"
                        >
                          {{ filename }}
                        </v-chip>
                        <div class="d-flex ga-1 flex-wrap">
                          <v-tooltip
                            :text="t('node.switchTitle')"
                            location="top"
                          >
                            <template #activator="{ props }">
                              <v-btn
                                icon="mdi-swap-horizontal"
                                size="x-small"
                                variant="text"
                                class="workspace-icon-btn"
                                :disabled="dialogBusy"
                                v-bind="props"
                                @click="switchWorkspaceWeight(filename)"
                              />
                            </template>
                          </v-tooltip>
                          <v-tooltip
                            :text="t('node.deleteWeightTitle')"
                            location="top"
                          >
                            <template #activator="{ props }">
                              <v-btn
                                icon="mdi-file-remove"
                                size="x-small"
                                variant="text"
                                class="workspace-icon-btn workspace-icon-btn--danger"
                                :disabled="dialogBusy"
                                v-bind="props"
                                @click="deleteWorkspaceWeight(filename)"
                              />
                            </template>
                          </v-tooltip>
                        </div>
                      </div>
                    </div>
                    <div v-else class="text-body-2 text-medium-emphasis">
                      {{ t("node.weightEmpty") }}
                    </div>
                  </v-card-text>
                </v-card>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-card class="card-ambient card-spacious" rounded="xl">
      <v-card-title class="text-subtitle-1 font-weight-bold">{{
        t("node.title")
      }}</v-card-title>
      <v-card-text class="table-shell">
        <v-data-table
          :headers="headers"
          :items="nodes"
          :loading="loading"
          :loading-text="t('common.loading')"
          :no-data-text="t('node.tableEmpty')"
          density="comfortable"
          item-value="id"
        >
          <template #item.status="{ item }">
            <v-chip
              :color="statusChipColor(item.status)"
              size="small"
              variant="flat"
            >
              {{ statusLabel(item.status) }}
            </v-chip>
          </template>

          <template #item.actions="{ item }">
            <div class="d-flex ga-1 flex-wrap action-strip">
              <v-tooltip :text="t('node.actionView')" location="top">
                <template #activator="{ props }">
                  <v-btn
                    icon="mdi-eye"
                    size="x-small"
                    variant="text"
                    v-bind="props"
                    @click="openDetail(item)"
                  />
                </template>
              </v-tooltip>
              <v-tooltip :text="t('node.actionEdit')" location="top">
                <template #activator="{ props }">
                  <v-btn
                    icon="mdi-pencil"
                    size="x-small"
                    variant="text"
                    v-bind="props"
                    @click="openEdit(item)"
                  />
                </template>
              </v-tooltip>
              <v-tooltip :text="t('node.actionSync')" location="top">
                <template #activator="{ props }">
                  <v-btn
                    icon="mdi-sync"
                    size="x-small"
                    variant="text"
                    v-bind="props"
                    @click="triggerSync(Number(item.id))"
                  />
                </template>
              </v-tooltip>
              <v-tooltip :text="t('node.actionParams')" location="top">
                <template #activator="{ props }">
                  <v-btn
                    icon="mdi-tune-variant"
                    size="x-small"
                    variant="text"
                    v-bind="props"
                    @click="triggerTemplate(Number(item.id))"
                  />
                </template>
              </v-tooltip>
              <v-tooltip :text="t('node.actionUploadWeight')" location="top">
                <template #activator="{ props }">
                  <v-btn
                    icon="mdi-upload"
                    size="x-small"
                    variant="text"
                    v-bind="props"
                    :loading="uploadingWeightNodeId === Number(item.id)"
                    :disabled="uploadingWeightNodeId !== null"
                    @click="triggerWeightUpload(Number(item.id))"
                  />
                </template>
              </v-tooltip>
              <v-tooltip :text="t('node.actionSwitchWeight')" location="top">
                <template #activator="{ props }">
                  <v-btn
                    icon="mdi-swap-horizontal"
                    size="x-small"
                    variant="text"
                    v-bind="props"
                    @click="triggerChangeWeight(Number(item.id))"
                  />
                </template>
              </v-tooltip>
              <v-tooltip :text="t('node.actionDeleteWeight')" location="top">
                <template #activator="{ props }">
                  <v-btn
                    icon="mdi-file-remove"
                    size="x-small"
                    variant="text"
                    v-bind="props"
                    @click="triggerDeleteWeight(Number(item.id))"
                  />
                </template>
              </v-tooltip>
              <v-tooltip :text="t('node.actionDelete')" location="top">
                <template #activator="{ props }">
                  <v-btn
                    icon="mdi-delete"
                    size="x-small"
                    variant="text"
                    v-bind="props"
                    @click="remove(Number(item.id))"
                  />
                </template>
              </v-tooltip>
            </div>
          </template>
        </v-data-table>

        <div class="d-flex justify-end mt-4">
          <v-pagination
            v-model="current"
            :length="pageCount"
            :total-visible="7"
            @update:model-value="loadNodes"
          />
        </div>
      </v-card-text>
    </v-card>
  </div>

  <v-dialog v-model="dialog" max-width="720">
    <v-card rounded="xl">
      <v-card-title>{{
        editId ? t("node.editNode") : t("node.createNode")
      }}</v-card-title>
      <v-card-text>
        <v-form ref="nodeFormRef" @submit.prevent="submitNode">
          <v-row dense>
            <v-col cols="12" md="6">
              <v-text-field
                v-model="form.nodeName"
                :label="t('node.nodeName')"
                :rules="nodeNameRules"
                variant="outlined"
              />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field
                v-model="form.host"
                :label="t('node.host')"
                :rules="hostRules"
                variant="outlined"
              />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field
                v-model="form.port"
                inputmode="numeric"
                :label="t('node.port')"
                :rules="portRules"
                variant="outlined"
              />
            </v-col>
            <v-col cols="12" md="3">
              <v-text-field
                v-model="form.httpProtocol"
                :label="t('node.protocol')"
                :rules="protocolRules"
                variant="outlined"
              />
            </v-col>
            <v-col cols="12" md="3">
              <v-text-field
                v-model="form.apiVersion"
                :label="t('node.apiVersion')"
                variant="outlined"
              />
            </v-col>
            <v-col cols="12">
              <v-textarea
                v-model="form.description"
                :label="t('node.description')"
                rows="3"
                variant="outlined"
              />
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn variant="text" @click="dialog = false">{{
          t("common.cancel")
        }}</v-btn>
        <v-btn color="primary" :loading="saving" @click="submitNode">{{
          t("common.save")
        }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>

  <ConfirmActionDialog
    v-model="confirmDialog.show"
    :cancel-text="t('common.cancel')"
    :confirm-text="t('common.delete')"
    :loading="dialogBusy"
    :message="confirmDialog.message"
    :title="confirmDialog.title"
    @confirm="confirmRemoveNode"
  />

  <PromptActionDialog
    v-model="promptDialog.show"
    :cancel-text="t('common.cancel')"
    :confirm-text="t('common.confirm')"
    :initial-value="promptDialog.value"
    :label="promptDialog.label"
    :loading="dialogBusy"
    :title="promptDialog.title"
    @confirm="submitPrompt"
  />

  <v-dialog v-model="templateDialog" max-width="860">
    <v-card rounded="xl">
      <v-card-title class="d-flex align-center ga-2">
        {{
          templateDialogMode === "create"
            ? t("node.templateCreate")
            : t("node.templateEdit")
        }}
        <v-spacer />
        <v-chip v-if="workspaceTitle" color="primary" variant="tonal">
          {{ workspaceTitle }}
        </v-chip>
      </v-card-title>
      <v-card-text>
        <v-row dense>
          <v-col cols="12" md="6">
            <v-text-field
              v-model="templateForm.templateName"
              :disabled="templateDialogMode === 'edit'"
              :label="t('node.templateName')"
              variant="outlined"
            />
          </v-col>
          <v-col cols="12" md="6">
            <v-switch
              v-model="templateForm.isActive"
              :label="t('node.templateActive')"
              color="primary"
              inset
            />
          </v-col>
          <v-col cols="12">
            <v-text-field
              v-model="templateForm.description"
              :label="t('node.templateDescription')"
              variant="outlined"
            />
          </v-col>
          <v-col cols="12">
            <v-textarea
              v-model="templateForm.paramsJson"
              :label="t('node.templateParamsJson')"
              auto-grow
              rows="12"
              variant="outlined"
            />
          </v-col>
          <v-col cols="12" v-if="templateFormError">
            <div class="text-caption text-error">{{ templateFormError }}</div>
          </v-col>
        </v-row>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn variant="text" @click="templateDialog = false">{{
          t("common.cancel")
        }}</v-btn>
        <v-btn
          color="primary"
          :loading="dialogBusy"
          @click="submitTemplateForm"
        >
          {{ t("common.save") }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>

  <v-dialog v-model="detailDialog" max-width="860">
    <v-card rounded="xl">
      <v-card-title class="d-flex align-center ga-2">
        {{ t("node.detail") }}
        <v-spacer />
        <v-btn
          v-if="detail"
          color="primary"
          size="small"
          variant="text"
          @click="copyNodeSummary(detail)"
        >
          {{ t("common.copy") }}
        </v-btn>
        <v-btn
          v-if="selectedNode"
          icon="mdi-refresh"
          size="small"
          variant="text"
          @click="selectedNode && openDetail(selectedNode)"
        />
      </v-card-title>
      <v-divider />
      <v-card-text>
        <v-progress-linear
          v-if="detailLoading"
          color="primary"
          indeterminate
          rounded
        />
        <v-card v-else-if="detail" variant="tonal">
          <v-card-title class="d-flex align-center ga-2">
            {{ t("node.detailSummary") }}
            <v-spacer />
            <v-btn
              v-if="detailRowCount(detail) > 16"
              color="primary"
              size="small"
              variant="text"
              @click="showAllNodeDetails = !showAllNodeDetails"
            >
              {{
                showAllNodeDetails ? t("common.showLess") : t("common.showAll")
              }}
            </v-btn>
          </v-card-title>
          <v-card-text>
            <div
              ref="nodeDetailScrollRef"
              class="node-detail-scroll scroll-elegant scroll-edge-hint"
              :class="{
                'scroll-at-top': scrollState.nodeDetail.atTop,
                'scroll-at-bottom': scrollState.nodeDetail.atBottom,
              }"
              @scroll.passive="handleScroll('nodeDetail', $event)"
            >
              <v-row dense>
                <v-col
                  v-for="item in detailRowsByMode(detail, showAllNodeDetails)"
                  :key="`node-detail-${item.key}`"
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
            </div>
          </v-card-text>
        </v-card>
        <div v-else class="text-body-2 text-medium-emphasis">
          {{ t("node.detailEmpty") }}
        </div>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.summary-card {
  min-height: 144px;
}

.workspace-node-select {
  width: min(420px, 100%);
}

.workspace-toolbar {
  display: grid;
  gap: 10px;
}

.workspace-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.workspace-list {
  display: grid;
  gap: 12px;
  max-height: 360px;
  overflow: auto;
  padding-right: 4px;
}

.workspace-item {
  overflow: hidden;
}

.workspace-icon-btn {
  background: rgba(31, 122, 140, 0.14);
  border: 1px solid rgba(31, 122, 140, 0.28);
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease;
}

.workspace-icon-btn:hover {
  background: rgba(31, 122, 140, 0.22);
  border-color: rgba(31, 122, 140, 0.4);
}

.workspace-icon-btn--danger {
  background: rgba(220, 38, 38, 0.12);
  border-color: rgba(220, 38, 38, 0.26);
}

.workspace-icon-btn--danger:hover {
  background: rgba(220, 38, 38, 0.2);
  border-color: rgba(220, 38, 38, 0.38);
}

.workspace-template-name--active {
  color: rgb(var(--v-theme-success));
}

.workspace-json {
  margin: 0;
  padding: 10px 12px;
  max-height: 180px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 0.8rem;
  line-height: 1.45;
  border-radius: 10px;
  background: rgba(18, 53, 66, 0.06);
  border: 1px solid rgba(31, 122, 140, 0.16);
}

.workspace-weight-wall {
  display: grid;
  gap: 10px;
  max-height: 360px;
  overflow: auto;
  padding-right: 4px;
}

.workspace-weight-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(18, 53, 66, 0.06);
  border: 1px solid rgba(31, 122, 140, 0.16);
}

.filter-row {
  row-gap: 8px;
}

.status-chip-wall {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.table-shell {
  padding-top: 16px;
}

.action-strip :deep(.v-btn) {
  border-radius: 10px;
}

.node-detail-scroll {
  max-height: 360px;
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

@media (max-width: 960px) {
  .workspace-list,
  .workspace-weight-wall {
    max-height: 280px;
  }

  .workspace-node-select {
    width: 100%;
  }
}
</style>
