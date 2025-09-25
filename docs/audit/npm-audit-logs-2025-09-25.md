# npm audit logs — 2025-09-25

Актуальные результаты `npm audit --json` перед апгрейдом инструментов (#79 → #91).

## apps/frontend
| Пакет | Advisory | Severity | Диапазон | Фикс |
|-------|----------|----------|----------|------|
| esbuild | [GHSA-67mh-4wv8-2f99](https://github.com/advisories/GHSA-67mh-4wv8-2f99) | moderate | `<=0.24.2` | обновить transitively через `vite >= 7.1.7` |
| vite | [GHSA-g4jq-h2w9-997c](https://github.com/advisories/GHSA-g4jq-h2w9-997c) | low | `<=5.4.19` | обновить `vite` до `7.1.7` |
| vite | [GHSA-jqfw-vq24-v9c3](https://github.com/advisories/GHSA-jqfw-vq24-v9c3) | low | `<=5.4.19` | обновить `vite` до `7.1.7` |

```
$ cd apps/frontend
$ npm audit --json > ../../tmp/frontend-audit-2025-09-25.json
```

## apps/miniapp-frontend
| Пакет | Advisory | Severity | Диапазон | Фикс |
|-------|----------|----------|----------|------|
| esbuild | [GHSA-67mh-4wv8-2f99](https://github.com/advisories/GHSA-67mh-4wv8-2f99) | moderate | `<=0.24.2` | обновить `vite` до `7.1.7` |

```
$ cd apps/miniapp-frontend
$ npm audit --json > ../../tmp/miniapp-frontend-audit-2025-09-25.json
```

> Полные JSON-логи сохранены локально в `tmp/` для дальнейших сравнений, но не включены в git.
