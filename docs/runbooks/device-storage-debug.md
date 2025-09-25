# Runbook — DeviceStorage Debug

Дата актуальности: 25 сентября 2025.

## Сценарии
- **Telegram WebApp (боевой)** — хранилище `Telegram.WebApp.CloudStorage` (лимит 1 MB на ключ).
- **Локальная разработка / Vite preview** — fallback к `window.localStorage`.

## Быстрый чекап
1. Откройте мини-апп в нужном окружении.
2. В DevTools выполните `Telegram.WebApp.CloudStorage?.isAvailable` (должно вернуть `true`).
3. Если `false`/`undefined`, приложение должно логировать `console.warn('CloudStorage API недоступен')` и работать через localStorage.

## Диагностика проблем
| Симптом | Проверка | Действие |
| --- | --- | --- |
| Черновики не сохраняются внутри Telegram | `Telegram.WebApp.CloudStorage.getItem('wau:profile-draft', console.log)` → `null` | Убедитесь, что мини-апп открыта по URL из BotFather и пользователь дал `write_access`. |
| Ошибка `DeviceStorageError: Размер данных превышает 1 MB лимит CloudStorage` | `new TextEncoder().encode(JSON.stringify(payload)).length` > 1_048_576 | Уменьшите размер сериализуемого состояния (обрезать фото/большие массивы). |
| В консоли `CloudStorage API недоступен` | `Telegram.WebApp.CloudStorage` отсутствует | Запустите внутри Telegram Desktop/Mobile; в браузере fallback штатный. |
| Fallback не срабатывает локально | `window.localStorage` недоступен (Safari Private, etc.) | Запустите в режиме без Private или используйте стейджинг в Telegram. |

## Окружение и переменные
- `.env`:
  - `BOT_TOKEN` — токен бота, используемый для авторизации и выставления WebApp.
  - `TELEGRAM_WEBAPP_URL` — публичный URL, зарегистрированный в BotFather (ngrok/Cloudflare Tunnel).
  - `JWT_SECRET` — требуется backend’у для подписи токенов (без него профили не загрузятся).
- Для локального fallback достаточно `npm run dev`; CloudStorage проявится только в официальном клиенте Telegram.

## Полезные команды
```js
// Проверка доступности CloudStorage
typeof Telegram?.WebApp?.CloudStorage !== 'undefined'

// Принудительная очистка черновика
Telegram.WebApp.CloudStorage.removeItem('wau:profile-draft', console.log)

// Логирование размеров
const draft = localStorage.getItem('wau:profile-draft');
new TextEncoder().encode(draft ?? '').length;
```

## Ссылки
- `apps/miniapp-frontend/src/services/deviceStorage.ts`
- Telegram Docs — [CloudStorage](https://core.telegram.org/bots/webapps#cloudstorage)
- Issue #78 и подзадачи #85–#90
