export interface UserProfile {
  id: number;
  username?: string;
  nickname?: string;
  email?: string;
  phoneNumber?: string;
  avatarUrl?: string;
  gender?: number;
  introduction?: string;
  lastLoginTime?: string;
  roles?: string[];
  permissions?: string[];
  [key: string]: unknown;
}
