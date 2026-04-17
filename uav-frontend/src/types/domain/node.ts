export interface YoloNode {
  id: number;
  nodeName?: string;
  host?: string;
  port?: string;
  status?: number | string;
  apiVersion?: string;
  httpProtocol?: string;
  activeWeightName?: string;
  description?: string;
  updatedAt?: string;
  [key: string]: unknown;
}
