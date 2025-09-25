# Mini App Frontend

React + Vite клиент Telegram Mini App для WAU Dating. Шаблон настроен под `@telegram-apps/sdk` и `@tma.js/sdk`, включает строгий TypeScript и готов к интеграции DeviceStorage/SecureStorage.

## Скрипты
- `npm run dev` — локальный запуск с Vite (порт 5174).
- `npm run build` — production сборка.
- `npm run preview` — предпросмотр сборки.
- `npm run lint` — проверка кода.
- `npm run test` — Vitest с jsdom окружением.

## Дизайн-токены
- Модуль `src/theme` собирает CSS-переменные на основе Telegram ThemeParams; вызывайте `applyThemeTokens` при старте приложения.
- Глобальные стили и компоненты используют `var(--tg-...)`; для inline-стилей доступен хелпер `themeVar('color', 'text')`.
- Fallback значения определены в `src/theme/base.css`, чтобы SSR/preview рендер имел корректную тему до инициализации Telegram SDK.

## TODO
- Подключить реальные API через gateway.
- Реализовать экраны onboarding, swipe deck, paywall.
- Интегрировать Telegram моки для unit/E2E тестов.
