# Railway Deployment Runbook

> Updated: 26 September 2025  
> Audience: geoBot maintainers who operate production/staging stacks on Railway without manual CLI deploys.

---

## Quick Outcome
- Каждый сервис (`backend`, `miniapp-gateway`, `frontend`, `miniapp-frontend`) автоматом деплоится из GitHub после зелёного CI, без локальных `railway up`.
- Preview-окружение появляется на каждый pull request и сверяется вместе с закрытием PR.
- Railpack собирает Maven/Node проекты без кастомных Dockerfile, прогоняя Liquibase перед поднятием Spring Boot.
- PostGIS сервис предоставляет приватное подключение; чувствительные значения лежат в sealed/reference variables. Используйте именно PostGIS-темплейт, иначе `CREATE EXTENSION postgis;` упадёт и оставит блокировку миграций.

---

## Service Map
| Service ID        | Root Directory         | Runtime        | Purpose                              |
|-------------------|------------------------|----------------|--------------------------------------|
| `backend`         | `apps/backend`         | Java 17 (Railpack) | Spring Boot API + Liquibase          |
| `miniapp-gateway` | `apps/miniapp-gateway` | Node 20 (Railpack) | Telegram Mini App proxy/gateway      |
| `frontend`        | `apps/frontend`        | Node 20 (Railpack) | Core React SPA                       |
| `miniapp-frontend`| `apps/miniapp-frontend`| Node 20 (Railpack) | Telegram Mini App UI                 |
| `postgis`         | template               | PostgreSQL 15 + PostGIS | Primary relational store (требуется PostGIS) |

---

## 0. Prerequisites
- Railway project with billing enabled and GitHub integration permitted.
- GitHub repository `geoBot` with workflows allowed to request OIDC or secrets usage.
- Maintainers have `railway` CLI ≥ 3.12 installed (требуется только для диагностики).
- Secrets (Bot token, JWT, external APIs) подготовлены для загрузки в Railway.
- База должна быть создана из шаблона **Postgres + PostGIS**. Если выбрать обычный `PostgreSQL`, миграции остановятся на `ERROR: extension "postgis" is not available` и оставят блокировку.

---

## 1. Проект и сервисы в Railway UI
1. Создайте/откройте проект `geoBot`.
2. Через **New → Database** добавьте `Postgres + PostGIS` шаблон, переименуйте сервис в `postgis`.
3. Через **New → Deploy from GitHub** подключите репозиторий, затем создайте сервисы:
   - `backend` → Root Directory `apps/backend`.
   - `miniapp-gateway` → Root Directory `apps/miniapp-gateway`.
   - `frontend` → Root Directory `apps/frontend`.
   - `miniapp-frontend` → Root Directory `apps/miniapp-frontend`.
4. В разделе **Deployments → Triggers** включите **Autodeploy on push** для ветки `main` и отметьте `Wait for CI` у всех сервисов, кроме `postgis`.
5. Если остались старые сервисы `PostgreSQL` без PostGIS, отключите для них autodeploy или удалите — достаточно одного `postgis`.
6. В **Settings → Private Networking** убедитесь, что включён проектный private domain; он понадобится для внутренних URL.

---

## 2. Config-as-Code (`railway.json`)
Сохраняем настройки сервисов в репозитории, чтобы изменения проходили ревью.

Создайте файлы:

`apps/backend/railway.json`
```json
{
  "$schema": "https://railway.com/railway.schema.json",
  "build": {
    "builder": "RAILPACK",
    "env": {
      "RAILPACK_JDK_VERSION": "17"
    },
    "buildCommand": "mvn -DskipTests package"
  },
  "deploy": {
    "healthcheckPath": "/actuator/health",
    "overlapSeconds": 30,
    "connections": [
      {
        "service": "PostgreSQL"
      }
    ]
  }
}
```

> Если понадобится отдельный pre-deploy шаг для Liquibase, добавьте `preDeployCommand` и настройте `liquibase-maven-plugin` в `pom.xml` с параметрами подключения (Railway передаёт переменные окружения в команду).

`apps/miniapp-gateway/railway.json`
```json
{
  "$schema": "https://railway.com/railway.schema.json",
  "build": {
    "builder": "RAILPACK",
    "buildCommand": "npm ci && npm run build"
  },
  "deploy": {
    "startCommand": "node dist/main.js",
    "healthcheckPath": "/health"
  }
}
```

> Убедитесь, что `npm run build` создаёт `dist/main.js`; добавьте отдельный build step в package.json, если необходимо.

`apps/frontend/railway.json`
```json
{
  "$schema": "https://railway.com/railway.schema.json",
  "build": {
    "builder": "RAILPACK",
    "buildCommand": "npm ci && npm run build"
  },
  "deploy": {
    "staticBuildPath": "dist"
  }
}
```

`apps/miniapp-frontend/railway.json`
```json
{
  "$schema": "https://railway.com/railway.schema.json",
  "build": {
    "builder": "RAILPACK",
    "buildCommand": "npm ci && npm run build"
  },
  "deploy": {
    "staticBuildPath": "dist"
  }
}
```

Добавьте файлы в Git и в Railway UI привяжите каждый сервис к своему `railway.json` (Settings → Config as Code).

---

## 3. Переменные окружения
Используйте Shared/Reference/Sealed переменные.

1. В проекте создайте Shared Variables:
   - `JWT_SECRET` (Sealed)
   - `BOT_TOKEN` (Sealed)
   - `OPEN_TELEMETRY_URL`
   - `S3_MEDIA_BUCKET` (если используется)
2. В сервисе `backend` добавьте:
   - `DATABASE_URL=${{postgis.DATABASE_URL}}`
   - `JWT_SECRET=${{shared.JWT_SECRET}}`
   - `BOT_TOKEN=${{shared.BOT_TOKEN}}`
   - `SPRING_PROFILES_ACTIVE=prod`
3. В `miniapp-gateway`:
   - `CORE_BASE_URL=https://${{backend.RAILWAY_PRIVATE_DOMAIN}}`
   - `BOT_TOKEN=${{shared.BOT_TOKEN}}`
4. В `frontend`:
   - `VITE_API_BASE_URL=https://${{backend.RAILWAY_PRIVATE_DOMAIN}}/api`
5. В `miniapp-frontend`:
   - `VITE_GATEWAY_BASE_URL=https://${{miniapp-gateway.RAILWAY_PUBLIC_DOMAIN}}`

> Любые токены/секреты помечаем как Sealed. Railway не покажет их в raw-редакторе и не скопирует в PR окружения.

---

## 4. GitHub CI → Railway Autodeploy
1. В разделе **Deployments → Autodeploy** убедитесь, что у всех сервисов стоит триггер «On push to main» + `Wait for CI`.
2. В GitHub создайте секреты `RAILWAY_TOKEN` и `RAILWAY_PROJECT_ID` (project-level token с правами Deploy + Variables) — понадобятся для preview action и вспомогательных шагов.
3. Добавьте workflow `.github/workflows/ci.yml`:

```yaml
name: CI
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
jobs:
  build-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - name: Backend tests
        run: mvn -f apps/backend/pom.xml test
      - uses: actions/setup-node@v4
        with:
          node-version: 20
      - name: Frontend build
        run: npm --prefix apps/frontend ci && npm --prefix apps/frontend run build
      - name: Miniapp Gateway build
        run: npm --prefix apps/miniapp-gateway ci && npm --prefix apps/miniapp-gateway run build
      - name: Miniapp Frontend build
        run: npm --prefix apps/miniapp-frontend ci && npm --prefix apps/miniapp-frontend run build
```

Railway дождётся успеха `build-test` перед запуском Railpack сборок.

---

## 5. Preview Environments
Добавьте workflow для pull request:

```yaml
name: PR Preview
on:
  pull_request:
    types: [opened, synchronize, reopened, closed]
jobs:
  deploy:
    if: github.event.action != 'closed'
    runs-on: ubuntu-latest
    steps:
      - name: Preview deploy
        uses: ayungavis/railway-preview-deploy-action@v1.0.2
        with:
          railway_api_token: ${{ secrets.RAILWAY_TOKEN }}
          project_id: ${{ secrets.RAILWAY_PROJECT_ID }}
          environment_name: production
          preview_environment_name: pr-${{ github.event.number }}
  cleanup:
    if: github.event.action == 'closed'
    runs-on: ubuntu-latest
    steps:
      - uses: ayungavis/railway-preview-deploy-action@v1.0.2
        with:
          railway_api_token: ${{ secrets.RAILWAY_TOKEN }}
          project_id: ${{ secrets.RAILWAY_PROJECT_ID }}
          environment_name: production
          preview_environment_name: pr-${{ github.event.number }}
          cleanup: 'true'
```

Action опубликует домен в комментарии к PR. При желании добавьте интеграционные тесты, запускаемые через `railway run` внутри этого workflow.

---

## 6. Post-Deploy Checks
- Проверяйте `/actuator/health` бэкенда и `/health` gateway через приватный домен; Railway UI показывает статус health-check.
- Убедитесь, что фронтенды собрали правильные base URL (см. Network tab).
- Включите `Log Drains` или прокиньте `OTEL_EXPORTER_OTLP_ENDPOINT` в backend для передачи метрик/логов внешним системам.
- Настройте алерты в Railway (Settings → Metrics) по CPU, памяти и ошибкам health-check.
- Если деплой падает с `extension "postgis" is not available`, значит база создана без PostGIS. Пересоздайте сервис из шаблона **Postgres + PostGIS** и обновите переменные подключения. После сбоя снимите блокировку:

```bash
railway connect PostgreSQL --environment production <<'SQL'
UPDATE databasechangeloglock
SET locked = FALSE,
    lockgranted = NULL,
    lockedby = NULL;
SQL
```

Это очистит lock, чтобы следующий запуск Liquibase прошёл успешно.

---

## 7. CLI Usage (опционально)
- `railway variables` — синхронизация `.env` ↔ Railway.
- `railway run <command>` — точечные миграции в preview окружениях.
- `railway up --ci` допускается только из GitHub Actions для hotfix, чтобы не обходить `Wait for CI`.

В повседневной работе избегайте локальных `railway up`, чтобы не задеплоить незаверенный код.

---

## 8. Telegram Bot Registration
1. В BotFather обновите Web App URL на `miniapp-frontend` публичный домен.
2. Для fallback-страницы укажите `frontend` домен (если используется полноценный кабинет).
3. Проверьте авторизацию и взаимодейтвие Mini App → Gateway → Backend в production окружении.

---

## 9. Инциденты и откаты
- Используйте **Deployments → Rollback** в Railway UI для возврата на предыдущую сборку.
- Храните Liquibase changelog под контролем версий; при откате отключайте auto migrations на сервисе (`preDeployCommand` → временно пустой) и разворачивайте резервную копию БД.
- В README допишите инструкции по восстановлению из snapshot PostGIS, если активировали автоматические бэкапы.
