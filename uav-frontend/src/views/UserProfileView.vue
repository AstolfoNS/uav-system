<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { onBeforeRouteLeave } from "vue-router";
import { fetchMyProfile, updateMyProfile, uploadAvatar } from "@/api";
import { notify } from "@/composables/useNotifier";
import type { UserProfile, UserProfileUpdateRequest } from "@/types";
import PageHero from "@/components/PageHero.vue";

const { t } = useI18n();

const loading = ref(false);
const saving = ref(false);
const uploading = ref(false);
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

const nicknameRules = computed(() => [
  (value: string) => !!value?.trim() || t("profile.nicknameRequired"),
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

const avatarPreview = computed(
  () => form.avatarUrl || "https://cdn.vuetifyjs.com/images/john.jpg",
);

interface ProfileComparableSnapshot {
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

  fillForm(serverProfile.value);
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

  const result = await formRef.value?.validate();
  if (!result?.valid) {
    return;
  }

  saving.value = true;
  try {
    const updated = await updateMyProfile(buildPayload());
    serverProfile.value = updated;
    fillForm(updated);
    notify(t("profile.saveSuccess"), "success");
  } catch (error) {
    notify(
      error instanceof Error ? error.message : t("profile.saveFailed"),
      "error",
    );
  } finally {
    saving.value = false;
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
        <v-card class="card-ambient card-spacious" rounded="xl">
          <v-card-title>{{ t("profile.avatar") }}</v-card-title>
          <v-card-text class="d-flex flex-column align-center ga-4">
            <v-avatar border color="primary" size="132" variant="tonal">
              <v-img :src="avatarPreview" cover />
            </v-avatar>

            <v-file-input
              accept="image/jpeg,image/jpg,image/png"
              :disabled="uploading"
              hide-details="auto"
              prepend-icon="mdi-camera"
              :label="t('profile.uploadAvatar')"
              variant="outlined"
              @update:model-value="onAvatarSelect"
            />

            <div class="text-caption text-medium-emphasis text-center">
              {{ t("profile.avatarTip") }}
            </div>
          </v-card-text>
        </v-card>

        <v-card class="card-ambient card-spacious mt-4" rounded="xl">
          <v-card-title>{{ t("profile.securityInfo") }}</v-card-title>
          <v-card-text>
            <v-text-field
              v-model="form.id"
              :label="t('profile.accountId')"
              readonly
              variant="outlined"
            />
            <v-text-field
              v-model="form.username"
              :label="t('profile.username')"
              readonly
              variant="outlined"
            />
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

        <v-row class="mt-1">
          <v-col cols="12" md="6">
            <v-card class="card-ambient" rounded="xl">
              <v-card-title>{{ t("profile.roles") }}</v-card-title>
              <v-card-text>
                <div
                  v-if="form.roles.length === 0"
                  class="text-medium-emphasis"
                >
                  {{ t("profile.noneRoles") }}
                </div>
                <div v-else class="d-flex flex-wrap ga-2">
                  <v-chip
                    v-for="role in form.roles"
                    :key="role"
                    color="primary"
                    size="small"
                    variant="tonal"
                  >
                    {{ role }}
                  </v-chip>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
          <v-col cols="12" md="6">
            <v-card class="card-ambient" rounded="xl">
              <v-card-title>{{ t("profile.permissions") }}</v-card-title>
              <v-card-text>
                <div
                  v-if="form.permissions.length === 0"
                  class="text-medium-emphasis"
                >
                  {{ t("profile.nonePermissions") }}
                </div>
                <div v-else class="d-flex flex-wrap ga-2">
                  <v-chip
                    v-for="permission in form.permissions"
                    :key="permission"
                    color="info"
                    size="small"
                    variant="tonal"
                  >
                    {{ permission }}
                  </v-chip>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-col>
    </v-row>
  </div>
</template>

<style scoped>
.profile-page {
  gap: 22px;
}
</style>
