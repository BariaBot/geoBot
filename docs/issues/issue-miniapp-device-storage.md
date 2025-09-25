# Issue: Завершить DeviceStorage для mini app черновиков

## Контекст
- Milestone: Sprint 2025-09-24
- GitHub Issue: https://github.com/jicool91/geoBot/issues/78
- Issue #65 закрывал базовую заглушку под Telegram DeviceStorage; текущая реализация уже переключается между CloudStorage и локальным fallback, требуется покрытие тестами и документация.
- Состояния профиля (`useProfileStore`) и матчей (`useMatchStore`) уже завязаны на асинхронный API, поэтому достаточно подключить реальное хранилище и обработку ошибок без переписывания потребителей.
- Требуется обеспечить корректную работу в следующих сценариях: WebApp внутри Telegram (основной кейс), Web preview (fallback) и локальная разработка без Telegram SDK.

## Задачи
- [ ] #85 Подключить CloudStorage API в DeviceStorage.
- [ ] #86 Реализовать fallback на localStorage и протестировать отказ CloudStorage.
- [ ] #87 Ограничить размер записей 1 MB и выдавать ошибки.
- [ ] #88 Логировать ошибки DeviceStorage через trackEvent.
- [x] #89 Покрыть DeviceStorage Vitest-тестами (успех/ошибка/fallback).
- [x] #90 Обновить документацию и session notes по DeviceStorage.

## Критерии готовности
- При запуске внутри Telegram WebApp черновики сохраняются и восстанавливаются через Telegram CloudStorage; локально всё работает через fallback без ошибок.
- Ошибки DeviceStorage фиксируются в логах и аналитике, а пользователю отображаются дружелюбные сообщения.
- Юнит-тесты мокающие CloudStorage проходят и защищают от регрессий.
- Документация описывает включение DeviceStorage и ограничения размера.

## Связанные материалы
- `apps/miniapp-frontend/src/services/deviceStorage.ts`
- `apps/miniapp-frontend/src/store/profile.ts`
- `apps/miniapp-frontend/src/store/match.ts`
- `apps/miniapp-frontend/src/utils/analytics.ts`
- Документы Telegram Mini App CloudStorage
- `docs/session-notes-2025-09-24.md`
