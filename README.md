# geoBot

Этот проект представляет собой Telegram-бот для знакомств, написанный на Spring Boot.

## Запуск

1. Установите PostgreSQL и создайте базу данных `dating_bot_db`.
2. Укажите параметры подключения к базе в `src/main/resources/application.properties` или через переменные окружения Spring.
3. Перед запуском задайте переменные окружения:
   - `BOT_USERNAME` – имя бота в Telegram (по умолчанию `GeoGreet_bot`).
   - `BOT_TOKEN` – токен, полученный у `@BotFather`.
   - `YANDEX_API_KEY` – ключ для работы с API Яндекса.

Соберите проект при помощи Maven:

```bash
mvn package
```

Запустите скомпилированный JAR:

```bash
java -jar target/datingBot-1.0.100.jar
```

## Запуск в Docker

Для запуска приложения и базы данных PostgreSQL можно воспользоваться `docker-compose`.
Соберите и поднимите сервисы командой:

```bash
docker-compose up --build
```

Перед первым запуском убедитесь, что задали переменные окружения `BOT_USERNAME`, `BOT_TOKEN` и `YANDEX_API_KEY` (например в файле `.env`).
