import { reactive } from "vue";

interface AuthSnapshot {
  accessToken: string;
  refreshToken: string;
}

const AUTH_STORAGE_KEY = "uav.auth.tokens";

export const authState = reactive<AuthSnapshot>({
  accessToken: "",
  refreshToken: "",
});

function loadFromStorage(): void {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return;
  }

  try {
    const parsed = JSON.parse(raw) as Partial<AuthSnapshot>;
    authState.accessToken = parsed.accessToken ?? "";
    authState.refreshToken = parsed.refreshToken ?? "";
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY);
  }
}

export function saveTokens(accessToken: string, refreshToken: string): void {
  authState.accessToken = accessToken;
  authState.refreshToken = refreshToken;
  localStorage.setItem(
    AUTH_STORAGE_KEY,
    JSON.stringify({ accessToken, refreshToken }),
  );
}

export function clearTokens(): void {
  authState.accessToken = "";
  authState.refreshToken = "";
  localStorage.removeItem(AUTH_STORAGE_KEY);
}

export function hasAccessToken(): boolean {
  return authState.accessToken.trim().length > 0;
}

export function getAccessToken(): string {
  return authState.accessToken;
}

export function getRefreshToken(): string {
  return authState.refreshToken;
}

loadFromStorage();
