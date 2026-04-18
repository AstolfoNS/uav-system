import { createI18n } from "vue-i18n";
import enUS from "@/locales/en-US";
import zhCN from "@/locales/zh-CN";

export const SUPPORTED_LOCALES = ["zh-CN", "en-US"] as const;
export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number];

const LOCALE_STORAGE_KEY = "uav.app.locale";

function resolveLocale(): SupportedLocale {
  const saved = localStorage.getItem(LOCALE_STORAGE_KEY);
  if (saved === "zh-CN" || saved === "en-US") {
    return saved;
  }

  const fromBrowser = navigator.language.toLowerCase();
  return fromBrowser.startsWith("zh") ? "zh-CN" : "en-US";
}

const i18n = createI18n({
  legacy: false,
  locale: resolveLocale(),
  fallbackLocale: "en-US",
  globalInjection: true,
  messages: {
    "zh-CN": zhCN,
    "en-US": enUS,
  },
});

export function setLocale(locale: SupportedLocale): void {
  i18n.global.locale.value = locale;
  localStorage.setItem(LOCALE_STORAGE_KEY, locale);
}

export default i18n;
