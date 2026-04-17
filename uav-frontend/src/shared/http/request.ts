import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  saveTokens,
} from "@/composables/useAuth";
import type { ApiEnvelope, AuthTokenResponse } from "@/types";
import i18n from "@/plugins/i18n";

const API_BASE_URL =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ??
  "http://localhost:8111/api/v1";

interface RequestOptions {
  auth?: boolean;
  body?: unknown;
  method?: "GET" | "POST" | "PUT" | "DELETE" | "PATCH";
  isFormData?: boolean;
  retry?: boolean;
  params?: QueryParams;
}

type QueryPrimitive = string | number | boolean;
type QueryValue = QueryPrimitive | null | undefined;
type QueryParams = URLSearchParams | object;

interface MethodOptions extends Omit<RequestOptions, "method"> {}

function toTokenPair(payload: AuthTokenResponse): {
  accessToken: string;
  refreshToken: string;
} {
  const accessToken = String(payload.accessToken ?? payload.access_token ?? "");
  const refreshToken = String(
    payload.refreshToken ?? payload.refresh_token ?? "",
  );

  if (!accessToken || !refreshToken) {
    throw new Error(i18n.global.t("errors.tokenPayloadInvalid"));
  }

  return { accessToken, refreshToken };
}

async function refreshAccessToken(): Promise<void> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    throw new Error(i18n.global.t("errors.refreshTokenMissing"));
  }

  const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
    method: "POST",
    headers: {
      "Refresh-Token": `Bearer ${refreshToken}`,
    },
  });

  const payload = (await response.json()) as ApiEnvelope<AuthTokenResponse>;

  if (!response.ok || payload.code !== 200) {
    throw new Error(payload.msg || i18n.global.t("errors.refreshFailed"));
  }

  const pair = toTokenPair(payload.data);
  saveTokens(pair.accessToken, pair.refreshToken);
}

function withQuery(path: string, params?: QueryParams): string {
  if (!params) {
    return path;
  }

  const search =
    params instanceof URLSearchParams
      ? new URLSearchParams(params)
      : new URLSearchParams();

  if (!(params instanceof URLSearchParams)) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== null && value !== undefined) {
        search.set(key, String(value as QueryValue));
      }
    }
  }

  const queryString = search.toString();
  if (!queryString) {
    return path;
  }

  const connector = path.includes("?") ? "&" : "?";
  return `${path}${connector}${queryString}`;
}

export async function apiRequest<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const {
    auth = true,
    body,
    method = "GET",
    isFormData = false,
    retry = true,
    params,
  } = options;

  const headers = new Headers();

  if (auth && getAccessToken()) {
    headers.set("Authorization", `Bearer ${getAccessToken()}`);
  }

  let requestBody: BodyInit | undefined;
  if (body !== undefined) {
    if (isFormData) {
      requestBody = body as FormData;
    } else {
      headers.set("Content-Type", "application/json");
      requestBody = JSON.stringify(body);
    }
  }

  const response = await fetch(`${API_BASE_URL}${withQuery(path, params)}`, {
    method,
    headers,
    body: requestBody,
  });

  if ((response.status === 401 || response.status === 403) && auth && retry) {
    try {
      await refreshAccessToken();
      return await apiRequest<T>(path, { ...options, retry: false });
    } catch {
      clearTokens();
      throw new Error(i18n.global.t("errors.sessionExpired"));
    }
  }

  const payload = (await response.json()) as ApiEnvelope<T>;

  if (!response.ok || payload.code !== 200) {
    throw new Error(
      payload.msg ||
        `${i18n.global.t("errors.requestFailed")} (${response.status})`,
    );
  }

  return payload.data;
}

export const request = {
  get<T>(path: string, options: MethodOptions = {}): Promise<T> {
    return apiRequest<T>(path, { ...options, method: "GET" });
  },
  post<T>(
    path: string,
    body?: unknown,
    options: MethodOptions = {},
  ): Promise<T> {
    return apiRequest<T>(path, { ...options, method: "POST", body });
  },
  put<T>(
    path: string,
    body?: unknown,
    options: MethodOptions = {},
  ): Promise<T> {
    return apiRequest<T>(path, { ...options, method: "PUT", body });
  },
  patch<T>(
    path: string,
    body?: unknown,
    options: MethodOptions = {},
  ): Promise<T> {
    return apiRequest<T>(path, { ...options, method: "PATCH", body });
  },
  delete<T>(path: string, options: MethodOptions = {}): Promise<T> {
    return apiRequest<T>(path, { ...options, method: "DELETE" });
  },
  upload<T>(
    path: string,
    formData: FormData,
    options: MethodOptions = {},
  ): Promise<T> {
    return apiRequest<T>(path, {
      ...options,
      method: "POST",
      body: formData,
      isFormData: true,
    });
  },
};

export function getApiBaseUrl(): string {
  return API_BASE_URL;
}
