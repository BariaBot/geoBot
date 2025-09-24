# Issue: Подготовить миграции Liquibase для payments и медиа

## Контекст
- Нужно хранить медиа-профили, историю покупок звёзд (`star_transactions`) и события по матчам (`match_events`).
- ChangeSet №11 уже добавлен в ветке `feature/liquibase-payments`.
- Стэш `temp-liquibase` очищен, текущая схема в main этого пока не знает.

## Задачи
1. Провести ревью и смёржить PR `feature/liquibase-payments` → main.
2. Убедиться, что `db.changelog-master.yml` подключает changeSet №11 и конфликтов с другими миграциями нет.
3. Запустить `mvn -f apps/backend/pom.xml clean test` и `docker compose -f infra/docker-compose.yml up` (при необходимости) для проверки миграций.
4. Обновить документацию/README, если появятся API, использующие новые таблицы.

## Критерии готовности
- PR смёржен, CI зелёный.
- Новые таблицы есть в базе после старта приложения.
- Нет незакрытых задач по этой схеме.

## Связанные материалы
- Ветка: `feature/liquibase-payments`
- Файл: `apps/backend/src/main/resources/db/changelog/db.changelog-master.yml`
- Сессионные заметки: `docs/session-notes-2025-09-24.md`
