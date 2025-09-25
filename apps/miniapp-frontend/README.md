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

## DeviceStorage
- Черновики профиля и просмотренных матчей сохраняются через хелпер `withDeviceStorage` (`src/services/deviceStorage.ts`).
- **Внутри Telegram**: если `window.Telegram.WebApp.CloudStorage` доступен, методы работы с черновиками используют облачное хранилище Telegram.
- **Локально и в web preview**: автоматически включается fallback на `localStorage`.
- CloudStorage ограничивает размер записи 1 MB. Перед сериализацией payload проверяется `TextEncoder().encode(value).length`.
- Ошибки выводятся в консоль (`console.warn`/`console.error`) и пробрасываются вызывающему коду; витальные сценарии должны перехватывать их и показывать пользователю уведомления.

### Как протестировать сохранение черновика

1. Запустите mini app локально: `npm install && npm run dev`.
2. Откройте `http://localhost:5174` в браузере. Вкладка «Профиль» будет использовать fallback на `localStorage`.
3. Чтобы проверить CloudStorage, опубликуйте билд на адрес, который добавлен в `BotFather` → `menuButton` → `web_app`. Заполните `.env` переменные `BOT_TOKEN`, `TELEGRAM_WEBAPP_URL` и перезапустите backend/gateway.
4. В Telegram (Desktop/Mobile) откройте WebApp. В DevTools (`Cmd+Option+J` на Desktop) выполните:
   ```js
   Telegram.WebApp.CloudStorage.getItem('wau:profile-draft', console.log)
   ```
   Должен вернуться JSON с черновиком профиля.

### Отладка
- Для форсированного fallback удалите объект `Telegram.WebApp.CloudStorage` в консоли и повторите действия: приложение должно переключиться на `localStorage` без ошибок.
- При ошибках CloudStorage обёртки выбросят `DeviceStorageError` — оберните вызовы в `try/catch` и логируйте `error.message`.
- Чтобы очистить черновики, используйте `localStorage.removeItem('wau:profile-draft')` или `Telegram.WebApp.CloudStorage.removeItem` внутри WebApp.

## TODO
- Подключить реальные API через gateway.
- Реализовать экраны onboarding, swipe deck, paywall.
- Интегрировать Telegram моки для unit/E2E тестов.
