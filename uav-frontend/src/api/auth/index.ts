import { request } from "@/shared/http/request";
import type { AuthLoginRequest, AuthTokenResponse } from "@/types";

export async function login(
  payload: AuthLoginRequest,
): Promise<AuthTokenResponse> {
  return request.post<AuthTokenResponse>("/auth/login", payload, {
    auth: false,
  });
}

export async function logout(): Promise<void> {
  await request.post<void>("/auth/logout");
}
