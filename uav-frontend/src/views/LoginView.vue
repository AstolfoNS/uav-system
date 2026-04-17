<script setup lang="ts">
import { reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import { login as loginApi } from "@/api";
import { saveTokens } from "@/composables/useAuth";
import { notify } from "@/composables/useNotifier";
import { setLocale, type SupportedLocale } from "@/plugins/i18n";

const router = useRouter();
const { t, locale } = useI18n();
const loading = ref(false);
const languageMenu = ref(false);

const form = reactive({
  username: "",
  password: "",
  rememberMe: true,
});

function resolveToken(raw: unknown): {
  accessToken: string;
  refreshToken: string;
} {
  const tokenBag = raw as Record<string, unknown>;
  const accessToken = String(
    tokenBag.accessToken ?? tokenBag.access_token ?? "",
  );
  const refreshToken = String(
    tokenBag.refreshToken ?? tokenBag.refresh_token ?? "",
  );

  if (!accessToken || !refreshToken) {
    throw new Error(t("login.invalidToken"));
  }

  return { accessToken, refreshToken };
}

async function submit(): Promise<void> {
  if (loading.value) {
    return;
  }

  if (!form.username.trim()) {
    notify(t("login.usernameRequired"), "warning");
    return;
  }

  if (!form.password) {
    notify(t("login.passwordRequired"), "warning");
    return;
  }

  loading.value = true;
  try {
    const data = await loginApi({
      username: form.username,
      password: form.password,
      rememberMe: form.rememberMe,
    });

    const pair = resolveToken(data);
    saveTokens(pair.accessToken, pair.refreshToken);
    notify(t("login.success"), "success");
    await router.push("/dashboard");
  } catch (error) {
    const message = error instanceof Error ? error.message : t("login.failed");
    notify(message, "error");
  } finally {
    loading.value = false;
  }
}

function changeLocale(value: SupportedLocale): void {
  setLocale(value);
}
</script>

<template>
  <v-app>
    <v-main
      class="d-flex align-center justify-center px-4 login-main"
      style="min-height: 100vh"
    >
      <v-card
        class="card-ambient login-card"
        max-width="468"
        width="100%"
        rounded="xl"
      >
        <v-card-text class="login-card-body">
          <div class="d-flex justify-end mb-2">
            <v-menu v-model="languageMenu" location="bottom end">
              <template #activator="{ props }">
                <v-btn
                  prepend-icon="mdi-translate"
                  size="small"
                  variant="tonal"
                  v-bind="props"
                >
                  {{
                    locale === "zh-CN"
                      ? t("common.chinese")
                      : t("common.english")
                  }}
                </v-btn>
              </template>
              <v-list density="compact" nav>
                <v-list-item
                  :active="locale === 'zh-CN'"
                  :title="t('common.chinese')"
                  @click="changeLocale('zh-CN')"
                />
                <v-list-item
                  :active="locale === 'en-US'"
                  :title="t('common.english')"
                  @click="changeLocale('en-US')"
                />
              </v-list>
            </v-menu>
          </div>

          <div class="d-flex align-center ga-3 mb-6">
            <v-avatar color="primary" size="44">
              <v-icon>mdi-helicopter</v-icon>
            </v-avatar>
            <div>
              <div class="text-h6 font-weight-bold">{{ t("login.title") }}</div>
              <div class="text-body-2 text-medium-emphasis">
                {{ t("login.subtitle") }}
              </div>
            </div>
          </div>

          <v-form @submit.prevent="submit">
            <v-text-field
              v-model="form.username"
              autocomplete="username"
              :label="t('login.username')"
              prepend-inner-icon="mdi-account"
              required
              variant="outlined"
            />
            <v-text-field
              v-model="form.password"
              autocomplete="current-password"
              :label="t('login.password')"
              prepend-inner-icon="mdi-lock"
              required
              type="password"
              variant="outlined"
            />

            <v-switch
              v-model="form.rememberMe"
              color="primary"
              hide-details
              :label="t('login.remember')"
            />

            <v-btn
              block
              class="mt-4 btn-primary-action"
              color="primary"
              :loading="loading"
              size="large"
              type="submit"
              variant="flat"
            >
              {{ t("login.submit") }}
            </v-btn>
          </v-form>

          <v-alert
            class="mt-5"
            color="info"
            icon="mdi-information-outline"
            type="info"
            variant="tonal"
          >
            {{ t("login.info") }}
          </v-alert>
        </v-card-text>
      </v-card>
    </v-main>
  </v-app>
</template>

<style scoped>
.login-main {
  background:
    radial-gradient(
      circle at 16% 12%,
      rgba(var(--uav-primary-rgb), 0.14),
      transparent 42%
    ),
    radial-gradient(
      circle at 90% 8%,
      rgba(var(--uav-primary-rgb), 0.12),
      transparent 34%
    );
}

.login-card {
  border: 1px solid rgba(var(--uav-primary-rgb), 0.24);
  box-shadow: 0 18px 44px rgba(var(--uav-primary-rgb), 0.18);
}

.login-card-body {
  padding: 34px 34px 30px;
}

@media (max-width: 600px) {
  .login-card-body {
    padding: 24px 20px 22px;
  }
}
</style>
