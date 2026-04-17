# i18n Key Convention

## Structure

- Locale root: `src/locales/<locale>/`
- Locale entry: `src/locales/<locale>/index.ts`
- Domain files: `app.ts`, `common.ts`, `login.ts`, `dashboard.ts`, `inference.ts`, `node.ts`, `records.ts`, `errors.ts`

## Key Prefix Rule

- Keys must be resolved with **domain prefix + local key**.
- Example: `records.table.status`, `login.submit`, `errors.sessionExpired`.
- Domain files must only define keys for their own domain.
- Cross-domain business key reuse is not allowed.
- Shared generic keys should be placed in `common.*`.

## Naming Rule

- Use lowerCamelCase for key names.
- Keep message text in locale values only.
- Do not embed hardcoded UI text in components when a key exists.

## Quality Gate

Run locale consistency and missing-key check:

```bash
pnpm run i18n:check
```

This script verifies:

- `en-US` and `zh-CN` have identical key paths
- all `t("...")` and `i18n.global.t("...")` keys used in `src/` exist in both locales
