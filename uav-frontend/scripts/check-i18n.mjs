import { readdir, readFile, stat } from "node:fs/promises";
import path from "node:path";
import process from "node:process";

const PROJECT_ROOT = process.cwd();
const SOURCE_ROOT = path.join(PROJECT_ROOT, "src");
const LOCALES_ROOT = path.join(SOURCE_ROOT, "locales");
const LOCALE_DOMAINS = [
  "app",
  "common",
  "dashboard",
  "errors",
  "inference",
  "login",
  "node",
  "profile",
  "records",
];

async function walkFiles(dirPath) {
  const entries = await readdir(dirPath, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    const fullPath = path.join(dirPath, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await walkFiles(fullPath)));
      continue;
    }
    files.push(fullPath);
  }

  return files;
}

function evaluateLocaleObject(sourceText) {
  const constMatch = sourceText.match(/const\s+\w+\s*=\s*/);
  if (!constMatch) {
    throw new Error("Locale file has no top-level const object");
  }

  const start = constMatch.index + constMatch[0].length;
  const endToken = "export default";
  const end = sourceText.lastIndexOf(endToken);
  if (end <= start) {
    throw new Error("Locale file has no export default statement");
  }

  const objectLiteral = sourceText
    .slice(start, end)
    .trim()
    .replace(/;\s*$/, "");
  return Function(`return (${objectLiteral});`)();
}

function flattenObject(obj, base = "") {
  const keys = [];
  for (const [key, value] of Object.entries(obj)) {
    const next = base ? `${base}.${key}` : key;
    if (value && typeof value === "object" && !Array.isArray(value)) {
      keys.push(...flattenObject(value, next));
    } else {
      keys.push(next);
    }
  }
  return keys;
}

async function loadLocale(localeName) {
  const locale = {};

  for (const domain of LOCALE_DOMAINS) {
    const filePath = path.join(LOCALES_ROOT, localeName, `${domain}.ts`);
    const content = await readFile(filePath, "utf8");
    locale[domain] = evaluateLocaleObject(content);
  }

  return locale;
}

function extractI18nKeys(content) {
  const keys = [];
  const pattern = /(?:\bi18n\.global\.)?\bt\(\s*["'`]([^"'`]+)["'`]\s*\)/g;
  let match = pattern.exec(content);

  while (match) {
    keys.push(match[1]);
    match = pattern.exec(content);
  }

  return keys;
}

function uniqueSorted(items) {
  return [...new Set(items)].sort((a, b) => a.localeCompare(b));
}

function printList(title, items) {
  if (items.length === 0) {
    return;
  }

  console.error(`\n${title}`);
  for (const item of items) {
    console.error(`- ${item}`);
  }
}

async function collectUsedKeys() {
  const files = await walkFiles(SOURCE_ROOT);
  const targetFiles = files.filter((filePath) => {
    if (filePath.includes(`${path.sep}locales${path.sep}`)) {
      return false;
    }
    return filePath.endsWith(".ts") || filePath.endsWith(".vue");
  });

  const allKeys = [];
  for (const filePath of targetFiles) {
    const content = await readFile(filePath, "utf8");
    const keys = extractI18nKeys(content);
    allKeys.push(...keys);
  }

  return uniqueSorted(allKeys);
}

async function main() {
  await stat(LOCALES_ROOT);

  const enUS = await loadLocale("en-US");
  const zhCN = await loadLocale("zh-CN");

  const enKeys = uniqueSorted(flattenObject(enUS));
  const zhKeys = uniqueSorted(flattenObject(zhCN));
  const usedKeys = await collectUsedKeys();

  const enSet = new Set(enKeys);
  const zhSet = new Set(zhKeys);

  const missingInZh = enKeys.filter((key) => !zhSet.has(key));
  const missingInEn = zhKeys.filter((key) => !enSet.has(key));

  const usedMissingInEn = usedKeys.filter((key) => !enSet.has(key));
  const usedMissingInZh = usedKeys.filter((key) => !zhSet.has(key));

  const hasError =
    missingInZh.length > 0 ||
    missingInEn.length > 0 ||
    usedMissingInEn.length > 0 ||
    usedMissingInZh.length > 0;

  if (hasError) {
    printList(
      "Locale drift: present in en-US but missing in zh-CN",
      missingInZh,
    );
    printList(
      "Locale drift: present in zh-CN but missing in en-US",
      missingInEn,
    );
    printList("Missing keys in en-US (used in source)", usedMissingInEn);
    printList("Missing keys in zh-CN (used in source)", usedMissingInZh);
    process.exit(1);
  }

  console.log("i18n check passed");
  console.log(`- Domains checked: ${LOCALE_DOMAINS.join(", ")}`);
  console.log(`- Keys in en-US: ${enKeys.length}`);
  console.log(`- Keys in zh-CN: ${zhKeys.length}`);
  console.log(`- Keys referenced in source: ${usedKeys.length}`);
}

main().catch((error) => {
  console.error("i18n check failed:", error);
  process.exit(1);
});
