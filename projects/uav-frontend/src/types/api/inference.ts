export interface InferenceRecordPageRequest {
  current: number;
  size: number;
  nodeId?: number | null;
  taskType?: number | null;
  originalFilename?: string;
}
