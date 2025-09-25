# Issue: Закрыть npm vulnerabilities в Vite/Vitest

## Контекст
- Milestone: Sprint 2025-09-24
- GitHub Issue: https://github.com/jicool91/geoBot/issues/79
- 25.09.2025 `npm audit` в `apps/miniapp-frontend` сообщает 4 moderate уязвимости (GHSA-67mh-4wv8-2f99, а также транзитивные предупреждения для `vite`, `vite-node` и `vitest` из-за того же advisories) из-за зависимостей `vite@5.2.0`, `vitest@1.6.0`, `vite-node` и транзитивного `esbuild`.
- `npm audit` в `apps/frontend` показывает 2 moderate проблемы: `vite` (GHSA-g4jq-h2w9-997c, GHSA-jqfw-vq24-v9c3) и транзитивный `esbuild` (GHSA-67mh-4wv8-2f99). Исправление доступно только в `vite@7.1.7` и `vitest@3.2.4`, что является мажорным обновлением.
- Нужно выработать план обновления, прогнать тесты и убедиться, что конфигурации Vite/ESLint/Vitest продолжают работать после апгрейда.

## Задачи
- [ ] #91 Зафиксировать текущие логи npm audit перед апгрейдом.
- [ ] #92 Обновить зависимости miniapp-frontend до безопасных версий Vite/Vitest.
- [ ] #93 Обновить зависимости основного фронтенда до безопасных версий Vite/Vitest.
- [ ] #94 Задокументировать результаты прогонов и breaking changes после апгрейда.
- [ ] #95 Обновить README/доки и session notes после апгрейда.

## Критерии готовности
- `npm audit` в `apps/frontend` и `apps/miniapp-frontend` завершает работу без уязвимостей.
- `package-lock.json` отражает новые версии зависимостей; CI (lint, test, build) проходит.
- Документация содержит упоминание об обновлении и новых требованиях к Node.js/инструментам.

## Связанные материалы
- `apps/frontend/package.json`
- `apps/miniapp-frontend/package.json`
- Логи `npm audit --json` от 25.09.2025
- `docs/session-notes-2025-09-24.md`

