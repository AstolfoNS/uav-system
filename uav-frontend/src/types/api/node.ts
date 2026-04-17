export interface YoloNodePageRequest {
  current: number;
  size: number;
  nodeName?: string;
  status?: number | null;
}

export interface YoloNodeUpsertRequest {
  nodeName: string;
  host: string;
  port: string;
  description?: string;
  httpProtocol?: string;
  apiVersion?: string;
}
