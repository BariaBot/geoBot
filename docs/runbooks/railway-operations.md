# Runbook: Railway Operations (MVP)

## Purpose
Поддержка сервисов Mini App на Railway до миграции в российское облако.

## Services
- `miniapp-frontend`
- `miniapp-gateway`
- `matching-core`
- `railway-postgres` (PostGIS)
- `railway-redis`

## Common Tasks
### 1. Рестарт сервиса
```
railway status
railway service restart <service-name>
```

### 2. Просмотр логов
```
timeout 10 railway logs --service <service-name>
```
Команда `railway logs` по умолчанию интерактивна и не поддерживает `--tail`; используем `timeout 10` для получения среза логов и предотвращения залипания CLI.
Фильтровать ошибки и тайм-ауты; при всплеске >5 ошибок/мин открыть инцидент в `#alerts-miniapp`.

### 3. Развёртывание
1. Запустить GitHub Actions workflow `Deploy <service>`.
2. После успеха выполнить `railway status` и убедиться, что rev обновился.
3. Проверить health-checks `/health` (gateway/core).

### 4. Бэкапы БД
```
railway snapshot create --service railway-postgres --name daily-backup
```
Скачать snapshot и загрузить в Object Storage (см. `deployment-and-migration.md`).

### 5. Управление переменными окружения
```
railway variables list
railway variables set KEY=VALUE
```
Секреты (TON keys, Stars) хранить в зашифрованном менеджере и ротировать ежеквартально.

## Incident Response
- **Симптомы:** рост ошибок 5xx, задержка >500 мс, сбой платежей.
- **Действия:**
  1. Проверить Railway статус и логи.
  2. Если проблема в БД — перезапустить подключение, проверить лимиты CPU/RAM.
  3. При аварии шлюза переключить Telegram bot ссылки на страницу статуса.
  4. Сообщить в `#alerts-miniapp`, задокументировать в постмортеме (Notion, TBD).

## Escalation Matrix
1. Дежурный инженёр Mini App Gateway.
2. Backend core owner.
3. DevOps (Railway/Cloud).

## Maintenance Windows
- Еженедельное окно: воскресенье 03:00–04:00 МСК (минимальный онлайн).
- Плановые обновления координируются через Trello/Linear.

## Notes
- Railway может «приспать» службы при неактивности; для gateway/core включить `Always On` (если бюджет позволяет) или настроить cron-ping.
- Если превышены лимиты 무료-tiers, сервисы останавливаются; следить за usage dashboard.

