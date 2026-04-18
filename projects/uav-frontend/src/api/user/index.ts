import { request } from "@/shared/http/request";
import type {
  UserPasswordUpdateRequest,
  UserProfile,
  UserProfileUpdateRequest,
} from "@/types";

export async function fetchMyProfile(): Promise<UserProfile> {
  return request.get<UserProfile>("/users/profile");
}

export async function updateMyProfile(
  payload: UserProfileUpdateRequest,
): Promise<UserProfile> {
  return request.put<UserProfile>("/users/profile", payload);
}

export async function updateMyPassword(
  payload: UserPasswordUpdateRequest,
): Promise<void> {
  await request.put<void>("/users/profile/password", payload);
}

export async function uploadAvatar(file: File): Promise<string> {
  const formData = new FormData();
  formData.append("file", file);

  return request.upload<string>("/files/upload/avatar", formData);
}
