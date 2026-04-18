export interface AuthTokenResponse {
  accessToken?: string;
  refreshToken?: string;
  access_token?: string;
  refresh_token?: string;
  [key: string]: unknown;
}

export interface AuthLoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}
