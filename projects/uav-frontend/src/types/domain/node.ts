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

export interface YoloNodeParamTemplate {
  id: number;
  nodeId: number;
  templateName: string;
  description?: string;
  isActive?: boolean;
  params: Record<string, unknown>;
  updatedAt?: string;
  [key: string]: unknown;
}

export interface YoloNodeWeightsSummary {
  activeWeight: string | null;
  availableWeights: string[];
}
