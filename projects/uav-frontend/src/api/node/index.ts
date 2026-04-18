import { request } from "@/shared/http/request";
import type {
  PagedResponse,
  YoloNode,
  YoloNodeParamCreateRequest,
  YoloNodeParamTemplate,
  YoloNodeParamUpdateRequest,
  YoloNodePageRequest,
  YoloNodeWeightsSummary,
  YoloNodeUpsertRequest,
} from "@/types";

export async function fetchNodePage(
  query: YoloNodePageRequest,
): Promise<PagedResponse<YoloNode>> {
  return request.get<PagedResponse<YoloNode>>("/yolo-nodes/page", {
    params: query,
  });
}

export async function fetchNodeDetail(
  nodeId: number,
): Promise<Record<string, unknown>> {
  return request.get<Record<string, unknown>>(`/yolo-nodes/${nodeId}`);
}

export async function createNode(
  payload: YoloNodeUpsertRequest,
): Promise<void> {
  await request.post<void>("/yolo-nodes", payload);
}

export async function updateNode(
  nodeId: number,
  payload: YoloNodeUpsertRequest,
): Promise<void> {
  await request.put<void>(`/yolo-nodes/${nodeId}`, payload);
}

export async function removeNode(nodeId: number): Promise<void> {
  await request.delete<void>(`/yolo-nodes/${nodeId}`);
}

export async function syncNode(nodeId: number): Promise<void> {
  await request.post<void>(`/yolo-nodes/${nodeId}/sync`);
}

export async function applyTemplate(
  nodeId: number,
  templateName: string,
): Promise<void> {
  await request.put<void>(
    `/yolo-nodes/${nodeId}/params/template/${templateName}/apply`,
  );
}

export async function fetchNodeParamTemplates(
  nodeId: number,
): Promise<YoloNodeParamTemplate[]> {
  return request.get<YoloNodeParamTemplate[]>(
    `/yolo-nodes/${nodeId}/params/templates`,
  );
}

export async function fetchNodeParamTemplate(
  nodeId: number,
  templateName: string,
): Promise<YoloNodeParamTemplate> {
  return request.get<YoloNodeParamTemplate>(
    `/yolo-nodes/${nodeId}/params/templates/${templateName}`,
  );
}

export async function createNodeParamTemplate(
  nodeId: number,
  payload: YoloNodeParamCreateRequest,
): Promise<void> {
  await request.post<void>(`/yolo-nodes/${nodeId}/params/templates`, payload);
}

export async function updateNodeParamTemplate(
  nodeId: number,
  templateName: string,
  payload: YoloNodeParamUpdateRequest,
): Promise<void> {
  await request.put<void>(
    `/yolo-nodes/${nodeId}/params/templates/${templateName}`,
    payload,
  );
}

export async function deleteNodeParamTemplate(
  nodeId: number,
  templateName: string,
): Promise<void> {
  await request.delete<void>(
    `/yolo-nodes/${nodeId}/params/templates/${templateName}`,
  );
}

export async function fetchNodeWeightSummary(
  nodeId: number,
): Promise<YoloNodeWeightsSummary> {
  return request.get<YoloNodeWeightsSummary>(`/yolo-nodes/${nodeId}/weights`);
}

export async function uploadWeight(nodeId: number, file: File): Promise<void> {
  const formData = new FormData();
  formData.append("file", file);

  await request.upload<void>(`/yolo-nodes/${nodeId}/weights`, formData);
}

export async function changeActiveWeight(
  nodeId: number,
  filename: string,
): Promise<void> {
  await request.put<void>(`/yolo-nodes/${nodeId}/weights/${filename}/active`);
}

export async function deleteWeight(
  nodeId: number,
  filename: string,
): Promise<void> {
  await request.delete<void>(`/yolo-nodes/${nodeId}/weights/${filename}`);
}
