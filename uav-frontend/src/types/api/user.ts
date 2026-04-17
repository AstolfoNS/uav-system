export interface UserProfileUpdateRequest {
  nickname: string;
  email?: string | null;
  phoneNumber?: string | null;
  avatarUrl?: string | null;
  gender?: 0 | 1 | 2 | null;
  introduction?: string | null;
}
