import { request } from "@/shared/http/request";
import type {
  InferenceRecordPageRequest,
  PagedResponse,
  YoloDetectionRecord,
} from "@/types";

export async function predictImage(
  nodeId: number,
  file: File,
): Promise<YoloDetectionRecord> {
  const formData = new FormData();
  formData.append("file", file);

  return request.upload<YoloDetectionRecord>(
    `/inference/nodes/${nodeId}/image`,
    formData,
  );
}

export async function predictVideo(
  nodeId: number,
  file: File,
): Promise<YoloDetectionRecord> {
  const formData = new FormData();
  formData.append("file", file);

  return request.upload<YoloDetectionRecord>(
    `/inference/nodes/${nodeId}/video`,
    formData,
  );
}

export async function fetchRecordPage(
  query: InferenceRecordPageRequest,
): Promise<PagedResponse<YoloDetectionRecord>> {
  return request.get<PagedResponse<YoloDetectionRecord>>(
    "/inference/records/page",
    { params: query },
  );
}

export async function fetchRecordDetail(
  recordId: number,
): Promise<YoloDetectionRecord> {
  return request.get<YoloDetectionRecord>(`/inference/records/${recordId}`);
}

export async function removeRecord(recordId: number): Promise<void> {
  await request.delete<void>(`/inference/records/${recordId}`);
}
