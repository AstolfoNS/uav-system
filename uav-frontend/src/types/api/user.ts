export interface UserProfileUpdateRequest {
  username: string;
  nickname: string;
  email?: string | null;
  phoneNumber?: string | null;
  avatarUrl?: string | null;
  gender?: 0 | 1 | 2 | null;
  introduction?: string | null;
}

export interface UserPasswordUpdateRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}
