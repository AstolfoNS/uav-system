export interface YoloDetectionRecord {
  id: number;
  nodeId?: number;
  taskType?: number;
  originalFilename?: string;
  status?: number | string;
  errorMessage?: string;
  detectionDetails?: unknown;
  createdAt?: string;
  updatedAt?: string;
  [key: string]: unknown;
}
