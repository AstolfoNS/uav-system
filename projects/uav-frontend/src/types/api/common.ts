export interface ApiEnvelope<T> {
  code: number;
  msg: string;
  data: T;
  details?: unknown;
}

export interface PagedResponse<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages?: number;
}
