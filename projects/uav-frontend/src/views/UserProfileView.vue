<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { onBeforeRouteLeave } from "vue-router";
import {
  fetchMyProfile,
  updateMyPassword,
  updateMyProfile,
  uploadAvatar,
} from "@/api";
import { notify } from "@/composables/useNotifier";
import type { UserProfile, UserProfileUpdateRequest } from "@/types";
import PageHero from "@/components/PageHero.vue";

const { t } = useI18n();

const loading = ref(false);
const saving = ref(false);
const uploading = ref(false);
const passwordSaving = ref(false);
const passwordDialog = ref(false);
const usernameServerError = ref("");
const avatarInputRef = ref<HTMLInputElement | null>(null);
const formRef = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(
  null,
);

const serverProfile = ref<UserProfile | null>(null);

const form = reactive({
  id: "",
  username: "",
  nickname: "",
  email: "",
  phoneNumber: "",
  avatarUrl: "",
  gender: 0 as 0 | 1 | 2,
  introduction: "",
  roles: [] as string[],
  permissions: [] as string[],
});

const passwordForm = reactive({
  currentPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const passwordVisibility = reactive({
  current: false,
  next: false,
  confirm: false,
});

type PasswordStrengthLevel = "weak" | "medium" | "strong";

function passwordStrengthScore(password: string): number {
  if (!password) {
    return 0;
  }

  let score = 0;
  if (password.length >= 6) {
    score += 1;
  }
  if (password.length >= 10) {
    score += 1;
  }
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) {
    score += 1;
  }
  if (/\d/.test(password)) {
    score += 1;
  }
  if (/[^A-Za-z0-9]/.test(password)) {
    score += 1;
  }

  return Math.min(score, 5);
}

const passwordStrengthLevel = computed<PasswordStrengthLevel>(() => {
  const score = passwordStrengthScore(passwordForm.newPassword);
  if (score >= 4) {
    return "strong";
  }
  if (score >= 2) {
    return "medium";
  }
  return "weak";
});

const passwordStrengthText = computed(() => {
  const level = passwordStrengthLevel.value;
  if (level === "strong") {
    return t("profile.passwordStrengthStrong");
  }
  if (level === "medium") {
    return t("profile.passwordStrengthMedium");
  }
  return t("profile.passwordStrengthWeak");
});

const passwordStrengthColor = computed(() => {
  const level = passwordStrengthLevel.value;
  if (level === "strong") {
    return "success";
  }
  if (level === "medium") {
    return "warning";
  }
  return "error";
});

const passwordStrengthProgress = computed(() => {
  const score = passwordStrengthScore(passwordForm.newPassword);
  return Math.max(10, score * 20);
});

const showPasswordMatchHint = computed(
  () =>
    passwordForm.newPassword.length > 0 ||
    passwordForm.confirmPassword.length > 0,
);

const passwordMatched = computed(
  () =>
    passwordForm.newPassword.length > 0 &&
    passwordForm.confirmPassword.length > 0 &&
    passwordForm.newPassword === passwordForm.confirmPassword,
);

const passwordMatchHintText = computed(() =>
  passwordMatched.value
    ? t("profile.passwordMatchHintMatched")
    : t("profile.passwordMatchHintMismatched"),
);

const passwordMatchHintColor = computed(() =>
  passwordMatched.value ? "success" : "error",
);

const nicknameRules = computed(() => [
  (value: string) => !!value?.trim() || t("profile.nicknameRequired"),
]);

const usernameRules = computed(() => [
  (value: string) => !!value?.trim() || t("profile.usernameRequired"),
  (value: string) =>
    /^[A-Za-z0-9_]{4,32}$/.test(value.trim())
      ? true
      : t("profile.usernameInvalid"),
]);

const emailRules = computed(() => [
  (value: string) => {
    const normalized = value.trim();
    if (!normalized) {
      return true;
    }

    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalized)
      ? true
      : t("profile.emailInvalid");
  },
]);

const phoneRules = computed(() => [
  (value: string) => {
    const normalized = value.trim();
    if (!normalized) {
      return true;
    }

    return /^1[3-9]\d{9}$/.test(normalized) ? true : t("profile.phoneInvalid");
  },
]);

const genderItems = computed(() => [
  { title: t("profile.genderUnknown"), value: 0 },
  { title: t("profile.genderMale"), value: 1 },
  { title: t("profile.genderFemale"), value: 2 },
]);

const avatarPreview = computed(() => form.avatarUrl.trim());
const hasAvatar = computed(() => avatarPreview.value.length > 0);
const roleList = computed(() =>
  Array.from(
    new Set(
      form.roles
        .map((item) => String(item).trim())
        .filter((item) => item.length > 0),
    ),
  ),
);
const permissionList = computed(() =>
  Array.from(
    new Set(
      form.permissions
        .map((item) => String(item).trim())
        .filter((item) => item.length > 0),
    ),
  ),
);
const displayName = computed(
  () => form.nickname.trim() || form.username.trim() || "-",
);
const lastLoginText = computed(() => {
  const value = String(serverProfile.value?.lastLoginTime ?? "").trim();
  return value || t("common.unavailable");
});

interface ProfileComparableSnapshot {
  username: string;
  nickname: string;
  email: string | null;
  phoneNumber: string | null;
  avatarUrl: string | null;
  gender: 0 | 1 | 2;
  introduction: string | null;
}

function normalizeGender(value: unknown): 0 | 1 | 2 {
  if (typeof value === "number") {
    return value === 1 || value === 2 ? value : 0;
  }

  const normalized = String(value ?? "").trim();
  if (normalized === "1") {
    return 1;
  }
  if (normalized === "2") {
    return 2;
  }
  return 0;
}

function asString(value: unknown): string {
  if (value === null || value === undefined) {
    return "";
  }
  return String(value);
}

function fillForm(profile: UserProfile): void {
  usernameServerError.value = "";
  form.id = asString(profile.id);
  form.username = asString(profile.username);
  form.nickname = asString(profile.nickname);
  form.email = asString(profile.email);
  form.phoneNumber = asString(profile.phoneNumber);
  form.avatarUrl = asString(profile.avatarUrl);
  form.gender = normalizeGender(profile.gender);
  form.introduction = asString(profile.introduction);
  form.roles = Array.isArray(profile.roles)
    ? profile.roles.map((item) => String(item))
    : [];
  form.permissions = Array.isArray(profile.permissions)
    ? profile.permissions.map((item) => String(item))
    : [];
}

function resetFormToServer(): void {
  if (!serverProfile.value) {
    return;
  }

  usernameServerError.value = "";
  fillForm(serverProfile.value);
}

function onUsernameInput(): void {
  if (usernameServerError.value) {
    usernameServerError.value = "";
  }
}

function getUsernameFieldError(error: unknown): string | null {
  if (!(error instanceof Error)) {
    return null;
  }

  const message = error.message.trim();
  const lowered = message.toLowerCase();
  const isDuplicate =
    message.includes("用户名已存在") ||
    (/username/.test(lowered) &&
      /(exist|exists|duplicate|duplicated|taken|already)/.test(lowered));

  return isDuplicate ? message : null;
}

function normalizeOptional(value: string): string | null {
  const normalized = value.trim();
  return normalized ? normalized : null;
}

function normalizeOptionalUnknown(value: unknown): string | null {
  const normalized = String(value ?? "").trim();
  return normalized ? normalized : null;
}

function buildSnapshotFromForm(): ProfileComparableSnapshot {
  return {
    username: form.username.trim(),
    nickname: form.nickname.trim(),
    email: normalizeOptional(form.email),
    phoneNumber: normalizeOptional(form.phoneNumber),
    avatarUrl: normalizeOptional(form.avatarUrl),
    gender: form.gender,
    introduction: normalizeOptional(form.introduction),
  };
}

function buildSnapshotFromProfile(
  profile: UserProfile,
): ProfileComparableSnapshot {
  return {
    username: String(profile.username ?? "").trim(),
    nickname: String(profile.nickname ?? "").trim(),
    email: normalizeOptionalUnknown(profile.email),
    phoneNumber: normalizeOptionalUnknown(profile.phoneNumber),
    avatarUrl: normalizeOptionalUnknown(profile.avatarUrl),
    gender: normalizeGender(profile.gender),
    introduction: normalizeOptionalUnknown(profile.introduction),
  };
}

const isDirty = computed(() => {
  if (!serverProfile.value) {
    return false;
  }

  return (
    JSON.stringify(buildSnapshotFromForm()) !==
    JSON.stringify(buildSnapshotFromProfile(serverProfile.value))
  );
});

function buildPayload(): UserProfileUpdateRequest {
  return buildSnapshotFromForm();
}

function normalizeAvatarSelection(file: File | File[] | null): File | null {
  if (Array.isArray(file)) {
    return file[0] ?? null;
  }
  return file;
}

function openAvatarPicker(): void {
  if (uploading.value) {
    return;
  }

  avatarInputRef.value?.click();
}

async function onAvatarInputChange(event: Event): Promise<void> {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0] ?? null;
  await onAvatarSelect(file);
  target.value = "";
}

function validateAvatarFile(file: File): boolean {
  const allowedTypes = ["image/jpeg", "image/jpg", "image/png"];
  const maxSize = 2 * 1024 * 1024;

  if (!allowedTypes.includes(file.type)) {
    notify(t("profile.avatarTypeInvalid"), "warning");
    return false;
  }

  if (file.size > maxSize) {
    notify(t("profile.avatarTooLarge"), "warning");
    return false;
  }

  return true;
}

async function onAvatarSelect(input: File | File[] | null): Promise<void> {
  const file = normalizeAvatarSelection(input);
  if (!file || uploading.value) {
    return;
  }

  if (!validateAvatarFile(file)) {
    return;
  }

  uploading.value = true;
  try {
    const url = await uploadAvatar(file);
    form.avatarUrl = url;
    notify(t("profile.uploadSuccess"), "success");
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("profile.uploadFailed"),
      "error",
    );
  } finally {
    uploading.value = false;
  }
}

async function loadProfile(): Promise<void> {
  loading.value = true;
  try {
    const profile = await fetchMyProfile();
    serverProfile.value = profile;
    fillForm(profile);
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("profile.fetchFailed"),
      "error",
    );
  } finally {
    loading.value = false;
  }
}

async function submit(): Promise<void> {
  if (saving.value) {
    return;
  }

  usernameServerError.value = "";

  const result = await formRef.value?.validate();
  if (!result?.valid) {
    return;
  }

  saving.value = true;
  try {
    const updated = await updateMyProfile(buildPayload());
    usernameServerError.value = "";
    serverProfile.value = updated;
    fillForm(updated);
    notify(t("profile.saveSuccess"), "success");
  } catch (error) {
    const fieldError = getUsernameFieldError(error);
    if (fieldError) {
      usernameServerError.value = fieldError;
      return;
    }

    notify(
      error instanceof Error ? error.message : t("profile.saveFailed"),
      "error",
    );
  } finally {
    saving.value = false;
  }
}

function resetPasswordForm(): void {
  passwordForm.currentPassword = "";
  passwordForm.newPassword = "";
  passwordForm.confirmPassword = "";
  passwordVisibility.current = false;
  passwordVisibility.next = false;
  passwordVisibility.confirm = false;
}

function openPasswordDialog(): void {
  resetPasswordForm();
  passwordDialog.value = true;
}

async function submitPasswordChange(): Promise<void> {
  if (passwordSaving.value) {
    return;
  }

  if (!passwordForm.currentPassword.trim()) {
    notify(t("profile.passwordRequired"), "warning");
    return;
  }

  if (
    !passwordForm.newPassword.trim() ||
    !passwordForm.confirmPassword.trim()
  ) {
    notify(t("profile.passwordRequired"), "warning");
    return;
  }

  if (passwordForm.newPassword.length < 6) {
    notify(t("profile.passwordMinLength"), "warning");
    return;
  }

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    notify(t("profile.passwordNotMatch"), "warning");
    return;
  }

  passwordSaving.value = true;
  try {
    await updateMyPassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword,
    });
    notify(t("profile.passwordSaveSuccess"), "success");
    passwordDialog.value = false;
    resetPasswordForm();
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("profile.passwordSaveFailed"),
      "error",
    );
  } finally {
    passwordSaving.value = false;
  }
}

function onBeforeUnload(event: BeforeUnloadEvent): void {
  if (!isDirty.value || saving.value) {
    return;
  }

  event.preventDefault();
  event.returnValue = "";
}

onBeforeRouteLeave(() => {
  if (!isDirty.value || saving.value) {
    return true;
  }

  return window.confirm(t("profile.unsavedLeaveConfirm"));
});

onMounted(async () => {
  await loadProfile();
  window.addEventListener("beforeunload", onBeforeUnload);
});

onBeforeUnmount(() => {
  window.removeEventListener("beforeunload", onBeforeUnload);
});
</script>

<template>
  <div class="page-shell profile-page">
    <PageHero :title="t('app.menu.profile')" :subtitle="t('profile.subtitle')">
      <template #actions>
        <v-btn
          class="btn-secondary-action"
          color="secondary"
          prepend-icon="mdi-restore"
          variant="tonal"
          @click="resetFormToServer"
        >
          {{ t("profile.resetToServer") }}
        </v-btn>
      </template>
    </PageHero>

    <v-row>
      <v-col cols="12" md="4">
        <v-card
          class="card-ambient card-spacious profile-side-card"
          rounded="xl"
        >
          <v-card-title>{{ t("profile.accountOverview") }}</v-card-title>
          <v-card-text
            class="d-flex flex-column align-center ga-4 profile-side-card__body"
          >
            <v-avatar
              v-if="hasAvatar"
              border
              color="primary"
              size="132"
              variant="tonal"
            >
              <v-img :src="avatarPreview" cover />
            </v-avatar>
            <div v-if="!hasAvatar" class="text-body-2 text-medium-emphasis">
              {{ t("profile.avatarEmpty") }}
            </div>

            <div
              class="text-center profile-display-name text-subtitle-1 font-weight-bold"
            >
              {{ displayName }}
            </div>
            <div class="text-caption text-medium-emphasis">
              @{{ form.username || "-" }}
            </div>

            <input
              ref="avatarInputRef"
              accept="image/jpeg,image/jpg,image/png"
              class="profile-file-input"
              type="file"
              @change="onAvatarInputChange"
            />

            <v-btn
              class="btn-secondary-action"
              color="secondary"
              :loading="uploading"
              prepend-icon="mdi-camera"
              variant="tonal"
              @click="openAvatarPicker"
            >
              {{ t("profile.avatarAction") }}
            </v-btn>

            <div class="text-caption text-medium-emphasis text-center">
              {{ t("profile.avatarTip") }}
            </div>

            <v-divider class="w-100 my-2" />

            <div class="profile-side-meta w-100">
              <div class="profile-side-meta__item">
                <span class="text-caption text-medium-emphasis">{{
                  t("profile.accountId")
                }}</span>
                <span class="text-body-2">{{ form.id || "-" }}</span>
              </div>
              <div class="profile-side-meta__item">
                <span class="text-caption text-medium-emphasis">{{
                  t("profile.lastLoginTime")
                }}</span>
                <span class="text-body-2">{{ lastLoginText }}</span>
              </div>
            </div>

            <v-btn
              class="btn-primary-action w-100"
              color="primary"
              prepend-icon="mdi-lock-reset"
              @click="openPasswordDialog"
            >
              {{ t("profile.changePassword") }}
            </v-btn>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="8">
        <v-card class="card-ambient card-spacious" rounded="xl">
          <v-card-title>{{ t("profile.basicInfo") }}</v-card-title>
          <v-card-text>
            <v-form
              ref="formRef"
              :disabled="loading || saving"
              @submit.prevent="submit"
            >
              <v-row>
                <v-col cols="12" md="6">
                  <v-text-field
                    v-model="form.username"
                    :label="t('profile.username')"
                    :error-messages="
                      usernameServerError ? [usernameServerError] : []
                    "
                    :rules="usernameRules"
                    required
                    variant="outlined"
                    @update:model-value="onUsernameInput"
                  />
                </v-col>
                <v-col cols="12" md="6">
                  <v-text-field
                    v-model="form.nickname"
                    :label="t('profile.nickname')"
                    :rules="nicknameRules"
                    required
                    variant="outlined"
                  />
                </v-col>
                <v-col cols="12" md="6">
                  <v-text-field
                    v-model="form.email"
                    :label="t('profile.email')"
                    :rules="emailRules"
                    variant="outlined"
                  />
                </v-col>
                <v-col cols="12" md="6">
                  <v-text-field
                    v-model="form.phoneNumber"
                    :label="t('profile.phoneNumber')"
                    :rules="phoneRules"
                    variant="outlined"
                  />
                </v-col>
                <v-col cols="12" md="6">
                  <v-select
                    v-model="form.gender"
                    :items="genderItems"
                    item-title="title"
                    item-value="value"
                    :label="t('profile.gender')"
                    variant="outlined"
                  />
                </v-col>
                <v-col cols="12">
                  <v-textarea
                    v-model="form.introduction"
                    :label="t('profile.introduction')"
                    rows="4"
                    variant="outlined"
                  />
                </v-col>
              </v-row>

              <div class="d-flex justify-end mt-2">
                <v-btn
                  class="btn-primary-action"
                  color="primary"
                  :loading="saving"
                  prepend-icon="mdi-content-save"
                  type="submit"
                  variant="elevated"
                >
                  {{ t("common.save") }}
                </v-btn>
              </div>
            </v-form>
          </v-card-text>
        </v-card>

        <v-card class="card-ambient mt-4" rounded="xl">
          <v-card-title class="d-flex align-center ga-2">
            <v-icon size="20">mdi-shield-account-outline</v-icon>
            {{ t("profile.accessSummary") }}
          </v-card-title>
          <v-card-text>
            <v-row class="access-grid" dense>
              <v-col cols="12" md="6">
                <div class="access-panel">
                  <div class="access-panel__head">
                    <div class="d-flex align-center ga-2">
                      <v-icon color="primary" size="18">mdi-account-group-outline</v-icon>
                      <span class="text-subtitle-2 font-weight-medium">
                        {{ t("profile.roles") }}
                      </span>
                    </div>
                    <v-chip color="primary" size="x-small" variant="flat">
                      {{ roleList.length }}
                    </v-chip>
                  </div>
                  <div v-if="roleList.length === 0" class="access-panel__empty text-medium-emphasis">
                    {{ t("profile.noneRoles") }}
                  </div>
                  <div v-else class="access-panel__body scroll-elegant">
                    <v-chip
                      v-for="role in roleList"
                      :key="role"
                      class="access-chip access-chip--role"
                      color="primary"
                      size="small"
                      variant="tonal"
                    >
                      {{ role }}
                    </v-chip>
                  </div>
                </div>
              </v-col>
              <v-col cols="12" md="6">
                <div class="access-panel">
                  <div class="access-panel__head">
                    <div class="d-flex align-center ga-2">
                      <v-icon color="info" size="18">mdi-key-chain-variant</v-icon>
                      <span class="text-subtitle-2 font-weight-medium">
                        {{ t("profile.permissions") }}
                      </span>
                    </div>
                    <v-chip color="info" size="x-small" variant="flat">
                      {{ permissionList.length }}
                    </v-chip>
                  </div>
                  <div
                    v-if="permissionList.length === 0"
                    class="access-panel__empty text-medium-emphasis"
                  >
                    {{ t("profile.nonePermissions") }}
                  </div>
                  <div v-else class="access-panel__body scroll-elegant">
                    <v-chip
                      v-for="permission in permissionList"
                      :key="permission"
                      class="access-chip access-chip--permission"
                      color="info"
                      size="small"
                      variant="tonal"
                    >
                      {{ permission }}
                    </v-chip>
                  </div>
                </div>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-dialog v-model="passwordDialog" max-width="560">
      <v-card rounded="xl">
        <v-card-title>{{ t("profile.passwordDialogTitle") }}</v-card-title>
        <v-card-text>
          <v-row>
            <v-col cols="12">
              <v-text-field
                v-model="passwordForm.currentPassword"
                :label="t('profile.currentPassword')"
                autocomplete="current-password"
                :append-inner-icon="
                  passwordVisibility.current ? 'mdi-eye-off' : 'mdi-eye'
                "
                :type="passwordVisibility.current ? 'text' : 'password'"
                variant="outlined"
                @click:append-inner="
                  passwordVisibility.current = !passwordVisibility.current
                "
              />
            </v-col>
            <v-col cols="12">
              <v-text-field
                v-model="passwordForm.newPassword"
                :label="t('profile.newPassword')"
                autocomplete="new-password"
                :append-inner-icon="
                  passwordVisibility.next ? 'mdi-eye-off' : 'mdi-eye'
                "
                :type="passwordVisibility.next ? 'text' : 'password'"
                variant="outlined"
                @click:append-inner="
                  passwordVisibility.next = !passwordVisibility.next
                "
              />
              <div class="password-strength mt-1">
                <div class="d-flex align-center justify-space-between">
                  <span class="text-caption text-medium-emphasis">{{
                    t("profile.passwordStrength")
                  }}</span>
                  <span
                    class="text-caption font-weight-medium"
                    :class="`text-${passwordStrengthColor}`"
                  >
                    {{ passwordStrengthText }}
                  </span>
                </div>
                <v-progress-linear
                  :color="passwordStrengthColor"
                  :model-value="passwordStrengthProgress"
                  height="6"
                  rounded
                />
              </div>
            </v-col>
            <v-col cols="12">
              <v-text-field
                v-model="passwordForm.confirmPassword"
                :label="t('profile.confirmPassword')"
                autocomplete="new-password"
                :append-inner-icon="
                  passwordVisibility.confirm ? 'mdi-eye-off' : 'mdi-eye'
                "
                :type="passwordVisibility.confirm ? 'text' : 'password'"
                variant="outlined"
                @click:append-inner="
                  passwordVisibility.confirm = !passwordVisibility.confirm
                "
              />
              <div
                v-if="showPasswordMatchHint"
                class="password-match-hint mt-1"
                :class="`text-${passwordMatchHintColor}`"
              >
                {{ passwordMatchHintText }}
              </div>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="passwordDialog = false">{{
            t("common.cancel")
          }}</v-btn>
          <v-btn
            color="primary"
            :loading="passwordSaving"
            prepend-icon="mdi-lock-check"
            @click="submitPasswordChange"
          >
            {{ t("profile.changePassword") }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.profile-page {
  gap: 22px;
}

.profile-side-card__body {
  padding-top: 20px;
}

.profile-display-name {
  line-height: 1.2;
}

.profile-file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.profile-side-meta {
  display: grid;
  gap: 8px;
}

.profile-side-meta__item {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(15, 76, 129, 0.05);
}

.password-strength {
  display: grid;
  gap: 6px;
}

.password-match-hint {
  font-size: 0.8rem;
  font-weight: 600;
}

.access-grid {
  row-gap: 12px;
}

.access-panel {
  border: 1px solid rgba(15, 76, 129, 0.15);
  border-radius: 12px;
  background: rgba(15, 76, 129, 0.04);
  min-height: 220px;
  display: flex;
  flex-direction: column;
}

.access-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 12px 10px;
  border-bottom: 1px solid rgba(15, 76, 129, 0.12);
}

.access-panel__body {
  padding: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 260px;
  overflow: auto;
}

.access-panel__empty {
  padding: 14px 12px;
}

.access-chip {
  border: 1px solid transparent;
}

.access-chip--role {
  border-color: rgba(15, 76, 129, 0.22);
}

.access-chip--permission {
  border-color: rgba(15, 147, 165, 0.3);
}
</style>
