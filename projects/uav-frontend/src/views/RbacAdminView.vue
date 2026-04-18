<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import {
  createRbacRole,
  fetchMyProfile,
  fetchRbacPermissions,
  fetchRbacRoles,
  fetchRbacUserPage,
  updateRolePermissions,
  updateUserRoles,
} from "@/api";
import { notify } from "@/composables/useNotifier";
import type { RbacPermission, RbacRole, RbacUser } from "@/types";
import PageHero from "@/components/PageHero.vue";

const { t } = useI18n();
const roleCodePattern = /^[a-z0-9]+(?::[a-z0-9]+)*$/;

const loading = ref(false);
const authorityLoading = ref(false);
const usersLoading = ref(false);
const rolesLoading = ref(false);
const savingUserRoles = ref(false);
const savingRolePermissions = ref(false);
const creatingRole = ref(false);
const authoritySet = ref<Set<string>>(new Set());

const users = ref<RbacUser[]>([]);
const roles = ref<RbacRole[]>([]);
const permissions = ref<RbacPermission[]>([]);

const userPager = reactive({
  current: 1,
  size: 10,
  total: 0,
  keyword: "",
});

const roleKeyword = ref("");

const userRoleDialog = ref(false);
const rolePermissionDialog = ref(false);
const createRoleDialog = ref(false);

const editingUser = ref<RbacUser | null>(null);
const editingRole = ref<RbacRole | null>(null);

const selectedRoleIds = ref<number[]>([]);
const selectedPermissionIds = ref<number[]>([]);

const createRoleForm = reactive({
  code: "",
  name: "",
  description: "",
  permissionIds: [] as number[],
});

const userHeaders = computed(() => {
  const headers = [
    { title: t("rbac.users.columns.username"), key: "username", width: 180 },
    { title: t("rbac.users.columns.nickname"), key: "nickname", width: 180 },
    { title: t("rbac.users.columns.roles"), key: "roles" },
  ];

  if (canUpdateUserRoles.value) {
    headers.push({
      title: t("rbac.users.columns.actions"),
      key: "actions",
      width: 150,
      sortable: false,
    });
  }

  return headers;
});

const roleHeaders = computed(() => {
  const headers = [
    { title: t("rbac.roles.columns.code"), key: "code", width: 180 },
    { title: t("rbac.roles.columns.name"), key: "name", width: 200 },
    { title: t("rbac.roles.columns.permissions"), key: "permissions" },
  ];

  if (canUpdateRolePermissions.value) {
    headers.push({
      title: t("rbac.roles.columns.actions"),
      key: "actions",
      width: 170,
      sortable: false,
    });
  }

  return headers;
});

const roleOptions = computed(() =>
  roles.value.map((role) => ({
    value: role.id,
    title: role.name?.trim() ? `${role.name} (${role.code})` : role.code,
  })),
);

const permissionOptions = computed(() =>
  permissions.value.map((permission) => ({
    value: permission.id,
    title: permission.name?.trim()
      ? `${permission.name} (${permission.code})`
      : permission.code,
  })),
);

const filteredRoles = computed(() => {
  const keyword = roleKeyword.value.trim().toLowerCase();
  if (!keyword) {
    return roles.value;
  }

  return roles.value.filter((role) => {
    const code = String(role.code ?? "").toLowerCase();
    const name = String(role.name ?? "").toLowerCase();
    return code.includes(keyword) || name.includes(keyword);
  });
});

const userPageCount = computed(() =>
  Math.max(1, Math.ceil(userPager.total / userPager.size)),
);

const userWithRolesCount = computed(
  () => users.value.filter((item) => item.roles?.length > 0).length,
);

const roleWithPermissionsCount = computed(
  () => roles.value.filter((item) => item.permissions?.length > 0).length,
);

const permissionCodeList = computed(() =>
  permissions.value.map((item) => item.code).filter(Boolean),
);

const normalizedCreateRoleCode = computed(() => createRoleForm.code.trim());

const isCreateRoleCodeValid = computed(
  () =>
    !normalizedCreateRoleCode.value ||
    roleCodePattern.test(normalizedCreateRoleCode.value),
);

const createRoleCodeErrors = computed(() =>
  isCreateRoleCodeValid.value ? [] : [t("rbac.dialogs.roleCodeRule")],
);

const selectedCreatePermissionCodes = computed(() => {
  const selected = new Set(createRoleForm.permissionIds);
  return permissions.value
    .filter((item) => selected.has(item.id))
    .map((item) => item.code)
    .filter(Boolean);
});

const canSubmitCreateRole = computed(
  () =>
    !!createRoleForm.code.trim() &&
    !!createRoleForm.name.trim() &&
    isCreateRoleCodeValid.value,
);

const canViewUserPage = computed(() => hasAuthority("rbac:user:page"));
const canViewRoles = computed(() => hasAuthority("rbac:role:list"));
const canViewPermissions = computed(() => hasAuthority("rbac:permission:list"));
const canCreateRole = computed(() => hasAuthority("rbac:role:create"));
const canUpdateUserRoles = computed(() => hasAuthority("rbac:user:role:update"));
const canUpdateRolePermissions = computed(() =>
  hasAuthority("rbac:role:permission:update"),
);

const hasAnyViewAuthority = computed(
  () => canViewUserPage.value || canViewRoles.value || canViewPermissions.value,
);

const isRbacReadOnly = computed(
  () =>
    hasAnyViewAuthority.value &&
    !canCreateRole.value &&
    !canUpdateUserRoles.value &&
    !canUpdateRolePermissions.value,
);

function hasAuthority(code: string): boolean {
  return authoritySet.value.has(code);
}

function resetUsersState(): void {
  users.value = [];
  userPager.total = 0;
}

function resetRolesState(): void {
  roles.value = [];
}

function resetPermissionsState(): void {
  permissions.value = [];
}

async function fetchAuthorities(): Promise<void> {
  authorityLoading.value = true;
  try {
    const profile = await fetchMyProfile();
    authoritySet.value = new Set(
      Array.isArray(profile.permissions)
        ? profile.permissions.map((item) => String(item))
        : [],
    );
  } finally {
    authorityLoading.value = false;
  }
}

async function fetchUsers(): Promise<void> {
  usersLoading.value = true;
  try {
    const page = await fetchRbacUserPage({
      current: userPager.current,
      size: userPager.size,
      keyword: userPager.keyword.trim() || undefined,
    });

    users.value = Array.isArray(page.records) ? page.records : [];
    userPager.total = Number(page.total ?? 0);
  } finally {
    usersLoading.value = false;
  }
}

async function fetchRoles(): Promise<void> {
  rolesLoading.value = true;
  try {
    const result = await fetchRbacRoles();
    roles.value = Array.isArray(result) ? result : [];
  } finally {
    rolesLoading.value = false;
  }
}

function openCreateRoleDialog(): void {
  if (!canCreateRole.value) {
    notify(t("rbac.common.noPermissionAction"), "warning");
    return;
  }
  createRoleDialog.value = true;
}

function closeCreateRoleDialog(): void {
  createRoleDialog.value = false;
  createRoleForm.code = "";
  createRoleForm.name = "";
  createRoleForm.description = "";
  createRoleForm.permissionIds = [];
}

function selectAllDefaultPermissions(): void {
  if (!canCreateRole.value) {
    return;
  }
  createRoleForm.permissionIds = permissions.value.map((item) => item.id);
}

function clearDefaultPermissions(): void {
  if (!canCreateRole.value) {
    return;
  }
  createRoleForm.permissionIds = [];
}

async function submitCreateRole(): Promise<void> {
  if (!canCreateRole.value || !canSubmitCreateRole.value) {
    return;
  }

  creatingRole.value = true;
  try {
    await createRbacRole({
      code: createRoleForm.code.trim(),
      name: createRoleForm.name.trim(),
      description: createRoleForm.description.trim() || undefined,
      permissionIds: createRoleForm.permissionIds,
    });
    notify(t("rbac.roles.createSuccess"), "success");
    closeCreateRoleDialog();
    await fetchRoles();
  } catch {
    notify(t("rbac.common.saveFailed"), "error");
  } finally {
    creatingRole.value = false;
  }
}

async function fetchPermissions(): Promise<void> {
  const result = await fetchRbacPermissions();
  permissions.value = Array.isArray(result) ? result : [];
}

async function loadAll(): Promise<void> {
  loading.value = true;
  try {
    await fetchAuthorities();

    if (!canViewUserPage.value) {
      resetUsersState();
    }

    if (!canViewRoles.value) {
      resetRolesState();
    }

    if (!canViewPermissions.value) {
      resetPermissionsState();
    }

    await Promise.all([
      canViewUserPage.value ? fetchUsers() : Promise.resolve(),
      canViewRoles.value ? fetchRoles() : Promise.resolve(),
      canViewPermissions.value ? fetchPermissions() : Promise.resolve(),
    ]);
  } catch {
    notify(t("rbac.common.loadingFailed"), "error");
  } finally {
    loading.value = false;
  }
}

function queryUsers(): void {
  if (!canViewUserPage.value) {
    return;
  }
  userPager.current = 1;
  void fetchUsers();
}

function resetUserQuery(): void {
  if (!canViewUserPage.value) {
    return;
  }
  userPager.keyword = "";
  userPager.current = 1;
  void fetchUsers();
}

function openUserRoleEditor(user: RbacUser): void {
  if (!canUpdateUserRoles.value) {
    notify(t("rbac.common.noPermissionAction"), "warning");
    return;
  }
  editingUser.value = user;
  selectedRoleIds.value = Array.isArray(user.roleIds) ? [...user.roleIds] : [];
  userRoleDialog.value = true;
}

function closeUserRoleEditor(): void {
  userRoleDialog.value = false;
  editingUser.value = null;
  selectedRoleIds.value = [];
}

async function submitUserRoles(): Promise<void> {
  if (!canUpdateUserRoles.value || !editingUser.value) {
    return;
  }

  savingUserRoles.value = true;
  try {
    await updateUserRoles(editingUser.value.id, {
      roleIds: selectedRoleIds.value,
    });

    notify(t("rbac.users.saveSuccess"), "success");
    closeUserRoleEditor();
    await fetchUsers();
  } catch {
    notify(t("rbac.common.saveFailed"), "error");
  } finally {
    savingUserRoles.value = false;
  }
}

function openRolePermissionEditor(role: RbacRole): void {
  if (!canUpdateRolePermissions.value) {
    notify(t("rbac.common.noPermissionAction"), "warning");
    return;
  }
  editingRole.value = role;
  selectedPermissionIds.value = Array.isArray(role.permissionIds)
    ? [...role.permissionIds]
    : [];
  rolePermissionDialog.value = true;
}

function closeRolePermissionEditor(): void {
  rolePermissionDialog.value = false;
  editingRole.value = null;
  selectedPermissionIds.value = [];
}

async function submitRolePermissions(): Promise<void> {
  if (!canUpdateRolePermissions.value || !editingRole.value) {
    return;
  }

  savingRolePermissions.value = true;
  try {
    await updateRolePermissions(editingRole.value.id, {
      permissionIds: selectedPermissionIds.value,
    });

    notify(t("rbac.roles.saveSuccess"), "success");
    closeRolePermissionEditor();
    await Promise.all([fetchRoles(), fetchUsers()]);
  } catch {
    notify(t("rbac.common.saveFailed"), "error");
  } finally {
    savingRolePermissions.value = false;
  }
}

onMounted(() => {
  void loadAll();
});
</script>

<template>
  <div class="rbac-page d-flex flex-column ga-5">
    <PageHero :title="t('rbac.title')" :subtitle="t('rbac.subtitle')">
      <template #actions>
        <v-btn
          color="primary"
          :loading="loading || authorityLoading"
          prepend-icon="mdi-refresh"
          variant="tonal"
          @click="loadAll"
        >
          {{ t("rbac.refresh") }}
        </v-btn>
      </template>
    </PageHero>

    <v-row class="rbac-kpi-row" dense>
      <v-col cols="12" md="4">
        <v-card class="card-ambient rbac-kpi-card" rounded="xl">
          <v-card-text>
            <div class="d-flex align-center ga-3">
              <v-avatar color="primary" size="40" variant="tonal">
                <v-icon>mdi-account-multiple-outline</v-icon>
              </v-avatar>
              <div>
                <div class="text-caption text-medium-emphasis">
                  {{ t("rbac.users.title") }}
                </div>
                <div class="text-h6 font-weight-bold">
                  {{ userPager.total }}
                </div>
              </div>
            </div>
            <div class="rbac-kpi-sub mt-2">
              {{ userWithRolesCount }} / {{ userPager.total }}
            </div>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="4">
        <v-card class="card-ambient rbac-kpi-card" rounded="xl">
          <v-card-text>
            <div class="d-flex align-center ga-3">
              <v-avatar color="secondary" size="40" variant="tonal">
                <v-icon>mdi-shield-account-outline</v-icon>
              </v-avatar>
              <div>
                <div class="text-caption text-medium-emphasis">
                  {{ t("rbac.roles.title") }}
                </div>
                <div class="text-h6 font-weight-bold">{{ roles.length }}</div>
              </div>
            </div>
            <div class="rbac-kpi-sub mt-2">
              {{ roleWithPermissionsCount }} / {{ roles.length }}
            </div>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="4">
        <v-card class="card-ambient rbac-kpi-card" rounded="xl">
          <v-card-text>
            <div class="d-flex align-center ga-3">
              <v-avatar color="success" size="40" variant="tonal">
                <v-icon>mdi-key-variant</v-icon>
              </v-avatar>
              <div>
                <div class="text-caption text-medium-emphasis">
                  {{ t("rbac.dialogs.selectPermissions") }}
                </div>
                <div class="text-h6 font-weight-bold">
                  {{ permissions.length }}
                </div>
              </div>
            </div>
            <div class="rbac-kpi-sub mt-2">API / MENU / BUTTON</div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-alert
      v-if="isRbacReadOnly"
      border="start"
      class="rbac-readonly-alert"
      color="info"
      icon="mdi-eye-lock-outline"
      variant="tonal"
    >
      <div class="font-weight-medium">{{ t("rbac.readonly.title") }}</div>
      <div class="text-body-2">{{ t("rbac.readonly.desc") }}</div>
    </v-alert>

    <v-row class="rbac-main-row">
      <v-col cols="12" lg="8">
        <v-card class="card-spacious rbac-panel" rounded="xl">
          <v-card-title class="rbac-panel-title d-flex align-center ga-2">
            <v-icon color="primary">mdi-account-multiple-check</v-icon>
            <span class="text-subtitle-1 font-weight-bold">{{
              t("rbac.users.title")
            }}</span>
            <v-spacer />
            <v-chip color="primary" size="small" variant="tonal">
              {{ userPager.total }}
            </v-chip>
          </v-card-title>

          <v-divider />

          <v-card-text class="rbac-toolbar-wrap">
            <div class="rbac-toolbar">
              <div class="rbac-toolbar__search">
                <v-text-field
                  v-model="userPager.keyword"
                  class="rbac-search"
                  density="comfortable"
                  hide-details
                  :label="t('rbac.users.search')"
                  :placeholder="t('rbac.users.searchPlaceholder')"
                  prepend-inner-icon="mdi-magnify"
                  variant="outlined"
                  @keyup.enter="queryUsers"
                />
              </div>
              <div class="rbac-toolbar__actions">
                <v-btn
                  color="primary"
                  :disabled="!canViewUserPage"
                  prepend-icon="mdi-magnify"
                  variant="flat"
                  @click="queryUsers"
                >
                  {{ t("rbac.users.query") }}
                </v-btn>
                <v-btn
                  :disabled="!canViewUserPage"
                  prepend-icon="mdi-backup-restore"
                  variant="tonal"
                  @click="resetUserQuery"
                >
                  {{ t("rbac.users.reset") }}
                </v-btn>
              </div>
            </div>
          </v-card-text>

          <v-divider />

          <v-data-table
            class="rbac-table"
            :headers="userHeaders"
            :items="users"
            :items-per-page="userPager.size"
            :loading="usersLoading"
            fixed-header
            height="490"
          >
            <template #item.nickname="{ item }">
              {{ item.nickname || "-" }}
            </template>

            <template #item.roles="{ item }">
              <div class="d-flex flex-wrap ga-1">
                <v-chip
                  v-for="roleCode in item.roles"
                  :key="roleCode"
                  color="primary"
                  size="small"
                  variant="tonal"
                >
                  {{ roleCode }}
                </v-chip>
                <span v-if="!item.roles?.length" class="text-medium-emphasis">
                  {{ t("rbac.users.noRoles") }}
                </span>
              </div>
            </template>

            <template #item.actions="{ item }">
              <v-btn
                v-if="canUpdateUserRoles"
                color="primary"
                prepend-icon="mdi-account-cog-outline"
                size="small"
                variant="tonal"
                @click="openUserRoleEditor(item)"
              >
                {{ t("rbac.users.configureRoles") }}
              </v-btn>
              <span v-else class="text-medium-emphasis">-</span>
            </template>

            <template #no-data>
              <div class="py-8 text-medium-emphasis text-center">
                {{ t("rbac.common.empty") }}
              </div>
            </template>
          </v-data-table>

          <v-divider />

          <v-card-actions class="justify-end pa-4">
            <v-pagination
              v-model="userPager.current"
              :length="userPageCount"
              :total-visible="7"
              @update:model-value="fetchUsers"
            />
          </v-card-actions>
        </v-card>
      </v-col>

      <v-col cols="12" lg="4" class="rbac-side-col">
        <div class="rbac-side-stack">
          <section class="rbac-flow-section" aria-label="role-step-1">
            <div class="rbac-flow-head">
              <div>
                <div class="rbac-flow-step">STEP 1</div>
                <div class="rbac-flow-title">
                  {{ t("rbac.flow.step1Title") }}
                </div>
                <div class="rbac-flow-desc">{{ t("rbac.flow.step1Desc") }}</div>
              </div>
              <v-btn
                v-if="canCreateRole"
                color="secondary"
                prepend-icon="mdi-plus"
                variant="tonal"
                @click="openCreateRoleDialog"
              >
                {{ t("rbac.roles.create") }}
              </v-btn>
            </div>

            <v-card
              class="card-spacious rbac-panel rbac-side-card"
              rounded="xl"
            >
              <v-card-title class="rbac-panel-title d-flex align-center ga-2">
                <v-icon color="secondary">mdi-shield-key-outline</v-icon>
                <span class="text-subtitle-1 font-weight-bold">
                  {{ t("rbac.roles.title") }}
                </span>
                <v-spacer />
                <v-chip color="secondary" size="small" variant="tonal">
                  {{ filteredRoles.length }}
                </v-chip>
              </v-card-title>

              <v-divider />

              <v-card-text class="rbac-toolbar-wrap pb-2">
                <v-text-field
                  v-model="roleKeyword"
                  class="rbac-search rbac-search--full"
                  density="comfortable"
                  hide-details
                  :label="t('rbac.roles.search')"
                  :placeholder="t('rbac.roles.searchPlaceholder')"
                  prepend-inner-icon="mdi-magnify"
                  variant="outlined"
                />
              </v-card-text>

              <v-data-table
                class="rbac-table"
                :headers="roleHeaders"
                :items="filteredRoles"
                :items-per-page="10"
                :loading="rolesLoading"
                fixed-header
                height="310"
              >
                <template #item.name="{ item }">
                  {{ item.name || "-" }}
                </template>

                <template #item.permissions="{ item }">
                  <div class="d-flex flex-wrap ga-1">
                    <v-chip
                      v-for="permissionCode in item.permissions"
                      :key="permissionCode"
                      color="success"
                      size="small"
                      variant="tonal"
                    >
                      {{ permissionCode }}
                    </v-chip>
                    <span
                      v-if="!item.permissions?.length"
                      class="text-medium-emphasis"
                    >
                      {{ t("rbac.roles.noPermissions") }}
                    </span>
                  </div>
                </template>

                <template #item.actions="{ item }">
                  <v-btn
                    v-if="canUpdateRolePermissions"
                    color="primary"
                    prepend-icon="mdi-key-link"
                    size="small"
                    variant="tonal"
                    @click="openRolePermissionEditor(item)"
                  >
                    {{ t("rbac.roles.configurePermissions") }}
                  </v-btn>
                  <span v-else class="text-medium-emphasis">-</span>
                </template>

                <template #no-data>
                  <div class="py-8 text-medium-emphasis text-center">
                    {{ t("rbac.common.empty") }}
                  </div>
                </template>
              </v-data-table>
            </v-card>
          </section>

          <div class="rbac-side-divider" aria-hidden="true"></div>

          <section class="rbac-flow-section" aria-label="role-step-2">
            <div class="rbac-flow-head">
              <div>
                <div class="rbac-flow-step">STEP 2</div>
                <div class="rbac-flow-title">
                  {{ t("rbac.flow.step2Title") }}
                </div>
                <div class="rbac-flow-desc">{{ t("rbac.flow.step2Desc") }}</div>
              </div>
            </div>

            <v-card
              class="card-spacious rbac-panel permission-catalog rbac-side-card"
              rounded="xl"
            >
              <v-card-title class="rbac-panel-title d-flex align-center ga-2">
                <v-icon color="success">mdi-key-chain-variant</v-icon>
                <span class="text-subtitle-1 font-weight-bold">
                  {{ t("rbac.dialogs.selectPermissions") }}
                </span>
                <v-spacer />
                <v-chip color="success" size="small" variant="tonal">
                  {{ permissionCodeList.length }}
                </v-chip>
              </v-card-title>

              <v-divider />

              <v-card-text class="permission-catalog__content scroll-elegant">
                <div
                  v-if="permissionCodeList.length"
                  class="d-flex flex-wrap ga-2"
                >
                  <v-chip
                    v-for="permissionCode in permissionCodeList"
                    :key="permissionCode"
                    color="success"
                    label
                    size="small"
                    variant="tonal"
                  >
                    {{ permissionCode }}
                  </v-chip>
                </div>
                <div v-else class="py-6 text-medium-emphasis text-center">
                  {{ t("rbac.common.empty") }}
                </div>
              </v-card-text>
            </v-card>
          </section>
        </div>
      </v-col>
    </v-row>

    <v-dialog v-model="createRoleDialog" max-width="620">
      <v-card rounded="xl">
        <v-card-title class="text-subtitle-1 font-weight-bold">
          {{ t("rbac.dialogs.createRoleTitle") }}
        </v-card-title>

        <v-card-text class="d-flex flex-column ga-3">
          <v-text-field
            v-model="createRoleForm.code"
            :label="t('rbac.dialogs.roleCode')"
            :hint="t('rbac.dialogs.roleCodeHint')"
            :error-messages="createRoleCodeErrors"
            maxlength="64"
            persistent-hint
            required
            variant="outlined"
          />
          <v-text-field
            v-model="createRoleForm.name"
            :label="t('rbac.dialogs.roleName')"
            maxlength="64"
            required
            variant="outlined"
          />
          <v-textarea
            v-model="createRoleForm.description"
            :label="t('rbac.dialogs.roleDescription')"
            maxlength="512"
            rows="3"
            variant="outlined"
          />

          <div class="create-role-perm-head">
            <div>
              <div class="text-body-2 font-weight-medium">
                {{ t("rbac.dialogs.defaultPermissions") }}
              </div>
              <div class="text-caption text-medium-emphasis">
                {{ t("rbac.dialogs.defaultPermissionsHint") }}
              </div>
            </div>
            <div class="create-role-perm-actions">
              <v-btn
                size="small"
                variant="text"
                @click="selectAllDefaultPermissions"
              >
                {{ t("rbac.dialogs.quickPickAllPermissions") }}
              </v-btn>
              <v-btn
                size="small"
                variant="text"
                @click="clearDefaultPermissions"
              >
                {{ t("rbac.dialogs.clearPickedPermissions") }}
              </v-btn>
            </div>
          </div>

          <v-autocomplete
            v-model="createRoleForm.permissionIds"
            chips
            clearable
            :items="permissionOptions"
            item-title="title"
            item-value="value"
            :label="t('rbac.dialogs.selectPermissions')"
            multiple
            variant="outlined"
          />

          <div
            v-if="selectedCreatePermissionCodes.length"
            class="create-role-perm-preview d-flex flex-wrap ga-2"
          >
            <v-chip
              v-for="permissionCode in selectedCreatePermissionCodes"
              :key="permissionCode"
              color="success"
              label
              size="small"
              variant="tonal"
            >
              {{ permissionCode }}
            </v-chip>
          </div>
        </v-card-text>

        <v-card-actions class="justify-end pa-4">
          <v-btn variant="text" @click="closeCreateRoleDialog">
            {{ t("rbac.dialogs.cancel") }}
          </v-btn>
          <v-btn
            color="secondary"
            :disabled="!canCreateRole || !canSubmitCreateRole"
            :loading="creatingRole"
            variant="flat"
            @click="submitCreateRole"
          >
            {{ t("rbac.dialogs.save") }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="userRoleDialog" max-width="680">
      <v-card rounded="xl">
        <v-card-title class="text-subtitle-1 font-weight-bold">
          {{ t("rbac.dialogs.userRoleTitle") }}
        </v-card-title>

        <v-card-text>
          <div class="text-body-2 mb-3 text-medium-emphasis">
            {{ editingUser?.username }}
          </div>
          <v-autocomplete
            v-model="selectedRoleIds"
            chips
            clearable
            :items="roleOptions"
            item-title="title"
            item-value="value"
            :label="t('rbac.dialogs.selectRoles')"
            multiple
            variant="outlined"
          />
        </v-card-text>

        <v-card-actions class="justify-end pa-4">
          <v-btn variant="text" @click="closeUserRoleEditor">
            {{ t("rbac.dialogs.cancel") }}
          </v-btn>
          <v-btn
            color="primary"
            :disabled="!canUpdateUserRoles"
            :loading="savingUserRoles"
            variant="flat"
            @click="submitUserRoles"
          >
            {{ t("rbac.dialogs.save") }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="rolePermissionDialog" max-width="760">
      <v-card rounded="xl">
        <v-card-title class="text-subtitle-1 font-weight-bold">
          {{ t("rbac.dialogs.rolePermissionTitle") }}
        </v-card-title>

        <v-card-text>
          <div class="text-body-2 mb-3 text-medium-emphasis">
            {{ editingRole?.code }}
          </div>
          <v-autocomplete
            v-model="selectedPermissionIds"
            chips
            clearable
            :items="permissionOptions"
            item-title="title"
            item-value="value"
            :label="t('rbac.dialogs.selectPermissions')"
            multiple
            variant="outlined"
          />
        </v-card-text>

        <v-card-actions class="justify-end pa-4">
          <v-btn variant="text" @click="closeRolePermissionEditor">
            {{ t("rbac.dialogs.cancel") }}
          </v-btn>
          <v-btn
            color="primary"
            :disabled="!canUpdateRolePermissions"
            :loading="savingRolePermissions"
            variant="flat"
            @click="submitRolePermissions"
          >
            {{ t("rbac.dialogs.save") }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.rbac-page {
  min-width: 0;
}

.rbac-kpi-row {
  margin-top: -6px;
}

.rbac-kpi-card {
  border: 1px solid rgba(var(--uav-primary-rgb), 0.18);
  background: linear-gradient(
    145deg,
    rgba(245, 251, 255, 0.92),
    rgba(232, 246, 252, 0.92)
  );
}

.rbac-kpi-sub {
  font-size: 0.78rem;
  color: rgba(22, 65, 86, 0.7);
}

.rbac-readonly-alert {
  margin-top: -10px;
}

.rbac-main-row {
  align-items: stretch;
  row-gap: 14px;
}

.rbac-side-col {
  display: flex;
}

.rbac-side-stack {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 12px;
  border-radius: 20px;
  border: 1px solid rgba(var(--uav-primary-rgb), 0.12);
  background: linear-gradient(
    180deg,
    rgba(238, 247, 253, 0.58),
    rgba(245, 250, 255, 0.72)
  );
}

.rbac-side-card {
  margin: 0;
}

.rbac-flow-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rbac-flow-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.rbac-flow-step {
  display: inline-flex;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: rgba(22, 65, 86, 0.65);
}

.rbac-flow-title {
  margin-top: 2px;
  font-size: 0.95rem;
  font-weight: 700;
  color: rgba(22, 65, 86, 0.92);
}

.rbac-flow-desc {
  margin-top: 2px;
  font-size: 0.78rem;
  color: rgba(22, 65, 86, 0.64);
}

.rbac-side-divider {
  height: 1px;
  background: linear-gradient(
    90deg,
    rgba(var(--uav-primary-rgb), 0),
    rgba(var(--uav-primary-rgb), 0.25),
    rgba(var(--uav-primary-rgb), 0)
  );
}

.rbac-panel {
  border: 1px solid rgba(var(--uav-primary-rgb), 0.18);
  box-shadow: 0 10px 24px rgba(var(--uav-primary-rgb), 0.08);
}

.rbac-panel-title {
  min-height: 58px;
}

.rbac-toolbar-wrap {
  padding-block: 14px;
}

.rbac-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.rbac-toolbar__search {
  flex: 1;
  min-width: 240px;
}

.rbac-toolbar__actions {
  display: flex;
  gap: 8px;
}

.rbac-search {
  min-width: 220px;
  max-width: 340px;
}

.rbac-search--full {
  min-width: 100%;
  max-width: 100%;
}

.rbac-table :deep(.v-data-table__wrapper) {
  border-top: 1px solid rgba(var(--uav-primary-rgb), 0.12);
}

.permission-catalog {
  flex: 1;
  min-height: 220px;
}

.permission-catalog__content {
  max-height: 240px;
  overflow: auto;
}

.create-role-perm-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.create-role-perm-actions {
  display: flex;
  gap: 6px;
}

.create-role-perm-preview {
  max-height: 120px;
  overflow: auto;
}

@media (max-width: 1280px) {
  .permission-catalog__content {
    max-height: 190px;
  }
}

@media (max-width: 960px) {
  .rbac-side-stack {
    padding: 8px;
    gap: 14px;
  }

  .rbac-flow-head {
    flex-direction: column;
    align-items: stretch;
  }

  .rbac-toolbar__search {
    min-width: 100%;
  }

  .create-role-perm-head {
    flex-direction: column;
  }

  .rbac-toolbar__actions {
    width: 100%;
  }

  .rbac-toolbar__actions > .v-btn {
    flex: 1;
  }
}
</style>
