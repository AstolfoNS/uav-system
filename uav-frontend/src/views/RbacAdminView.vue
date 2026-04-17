<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import {
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

const loading = ref(false);
const usersLoading = ref(false);
const rolesLoading = ref(false);
const savingUserRoles = ref(false);
const savingRolePermissions = ref(false);

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

const editingUser = ref<RbacUser | null>(null);
const editingRole = ref<RbacRole | null>(null);

const selectedRoleIds = ref<number[]>([]);
const selectedPermissionIds = ref<number[]>([]);

const userHeaders = computed(() => [
  { title: t("rbac.users.columns.username"), key: "username", width: 180 },
  { title: t("rbac.users.columns.nickname"), key: "nickname", width: 180 },
  { title: t("rbac.users.columns.roles"), key: "roles" },
  {
    title: t("rbac.users.columns.actions"),
    key: "actions",
    width: 150,
    sortable: false,
  },
]);

const roleHeaders = computed(() => [
  { title: t("rbac.roles.columns.code"), key: "code", width: 180 },
  { title: t("rbac.roles.columns.name"), key: "name", width: 200 },
  { title: t("rbac.roles.columns.permissions"), key: "permissions" },
  {
    title: t("rbac.roles.columns.actions"),
    key: "actions",
    width: 170,
    sortable: false,
  },
]);

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

async function fetchPermissions(): Promise<void> {
  const result = await fetchRbacPermissions();
  permissions.value = Array.isArray(result) ? result : [];
}

async function loadAll(): Promise<void> {
  loading.value = true;
  try {
    await Promise.all([fetchUsers(), fetchRoles(), fetchPermissions()]);
  } catch {
    notify(t("rbac.common.loadingFailed"), "error");
  } finally {
    loading.value = false;
  }
}

function queryUsers(): void {
  userPager.current = 1;
  void fetchUsers();
}

function resetUserQuery(): void {
  userPager.keyword = "";
  userPager.current = 1;
  void fetchUsers();
}

function openUserRoleEditor(user: RbacUser): void {
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
  if (!editingUser.value) {
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
  if (!editingRole.value) {
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
          :loading="loading"
          prepend-icon="mdi-refresh"
          variant="tonal"
          @click="loadAll"
        >
          {{ t("rbac.refresh") }}
        </v-btn>
      </template>
    </PageHero>

    <v-row dense class="ga-0">
      <v-col cols="12" lg="7">
        <v-card class="card-spacious h-100" rounded="xl">
          <v-card-title class="d-flex flex-wrap align-center ga-2">
            <span class="text-subtitle-1 font-weight-bold">
              {{ t("rbac.users.title") }}
            </span>
            <v-spacer />
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
            <v-btn color="primary" variant="flat" @click="queryUsers">
              {{ t("rbac.users.query") }}
            </v-btn>
            <v-btn variant="text" @click="resetUserQuery">
              {{ t("rbac.users.reset") }}
            </v-btn>
          </v-card-title>

          <v-divider />

          <v-data-table
            :headers="userHeaders"
            :items="users"
            :items-per-page="userPager.size"
            :loading="usersLoading"
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
                color="primary"
                size="small"
                variant="text"
                @click="openUserRoleEditor(item)"
              >
                {{ t("rbac.users.configureRoles") }}
              </v-btn>
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

      <v-col cols="12" lg="5">
        <v-card class="card-spacious h-100" rounded="xl">
          <v-card-title class="d-flex flex-wrap align-center ga-2">
            <span class="text-subtitle-1 font-weight-bold">
              {{ t("rbac.roles.title") }}
            </span>
            <v-spacer />
            <v-text-field
              v-model="roleKeyword"
              class="rbac-search"
              density="comfortable"
              hide-details
              :label="t('rbac.roles.search')"
              :placeholder="t('rbac.roles.searchPlaceholder')"
              prepend-inner-icon="mdi-magnify"
              variant="outlined"
            />
          </v-card-title>

          <v-divider />

          <v-data-table
            :headers="roleHeaders"
            :items="filteredRoles"
            :items-per-page="10"
            :loading="rolesLoading"
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
                color="primary"
                size="small"
                variant="text"
                @click="openRolePermissionEditor(item)"
              >
                {{ t("rbac.roles.configurePermissions") }}
              </v-btn>
            </template>

            <template #no-data>
              <div class="py-8 text-medium-emphasis text-center">
                {{ t("rbac.common.empty") }}
              </div>
            </template>
          </v-data-table>
        </v-card>
      </v-col>
    </v-row>

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

.rbac-search {
  min-width: 220px;
  max-width: 340px;
}
</style>
