# Mini App Gateway

NestJS слой для Telegram Mini App. Валидация `initData`, Stars/TON webhooks, прокси-запросы в core (Spring) и подготовка к WebSocket.

## Скрипты
- `npm run start:dev` — локальный запуск с hot reload.
- `npm run build` — билд в `dist/`.
- `npm start` — запуск собранной версии.
- `npm run lint` — ESLint проверка.

## TODO
- Реальная интеграция с core API (`CORE_BASE_URL`).
- Подпись/валидация Telegram Stars и TON Connect.
- Реализация WebSocket-шины и очередей BullMQ.
