# Dating Mini App

Монорепозиторий с бэкендом на Spring Boot и фронтендом на Vite. Бэкенд использует Liquibase и PostGIS.

Реализованы базовые функции:
- авторизация по Telegram и выдача JWT;
- управление профилем пользователя;
- обновление геолокации и поиск анкет поблизости;
- лайки и автоматическое создание матчей.
- запросы на встречу с подтверждением.
- чат в режиме реального времени через WebSocket.
- VIP подписка и платежи через Telegram Stars (заглушка).
- адаптивный фронтенд с маршрутизацией, нижней навигацией с иконками и поддержкой темы Telegram.
- фронтенд использует Zustand для хранения состояния и React Query с индикатором загрузки.
- страница поиска показывает карточку анкеты с кнопками лайка и пропуска, сопровождаемыми лёгким haptic-откликом.
- действия лайк/пропуск сопровождаются короткими всплывающими подсказками.

## Требования к окружению

- Node.js ≥ 20.19.0 (или ≥ 22.12.0); Vite 7 требует актуальный LTS, рекомендуем `node@20.19`.
- npm 10 (идёт в комплекте с Node 20 LTS).
- Java 17 для Spring Boot.

## Frontend

- Дизайн-токены описаны в `apps/frontend/src/theme/tokens.ts` и автоматически применяются через CSS Custom Properties.
- Хук `useTelegramTheme` синхронизирует токены с параметрами темы Telegram (`themeChanged`) и задаёт дефолты для разработки вне Mini App.
- Навигация, карточки и уведомления используют только токены вместо жёстких цветов; кастомизация темы происходит без перезагрузки.
- Для проверки темизации добавлен Vitest: `npm run test`.
- VIP paywall (`/vip`) собирает тарифы, проверяет подписку через Stars и поддерживает автотесты на основной сценарий.
- Черновики форм сохраняются через Telegram DeviceStorage с fallback на localStorage и автотестом обёртки.
- Страница профиля позволяет редактировать данные, управлять фото с drag-n-drop и хранит черновики в DeviceStorage.

## Быстрый старт

```bash
# установка актуальной версии Node (через corepack)
corepack enable
corepack use npm@10
node -v   # 20.19.x
npm -v    # 10.x

# сборка и запуск бэкенда
mvn -q -f apps/backend/pom.xml spring-boot:run

# запуск фронтенда
cd apps/frontend && npm install && npm run dev
```

Также доступен docker-compose:

```bash
docker compose -f infra/docker-compose.yml up --build
```

Документация API доступна по адресу `http://localhost:8080/swagger-ui.html` после запуска бэкенда.
Мониторинг доступен через Spring Boot Actuator: проверка здоровья — `http://localhost:8080/actuator/health`, метрики — `http://localhost:8080/actuator/metrics`.

## Railway Deployment

- Автодеплой происходит через Railpack из GitHub после успешного CI; подробная инструкция — `docs/runbooks/railway-deploy.md`.
- Каждая служба (`apps/backend`, `apps/miniapp-gateway`, `apps/frontend`, `apps/miniapp-frontend`) содержит `railway.json` с билд/старт-командами для Railway.
- В Railway UI подключите репозиторий, включите `Autodeploy on push` + `Wait for CI` на ветке `main` и привяжите соответствующий `railway.json` в разделе Config as Code.
- Production/preview переменные храните в sealed/reference variables; токены и секреты прокиньте через `RAILWAY_TOKEN` и `RAILWAY_PROJECT_ID` в GitHub Actions.
