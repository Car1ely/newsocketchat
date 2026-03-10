# Docker Guide

Руководство по запуску Multi-Protocol Chat System в Docker контейнерах.

## Быстрый старт

### 1. Запуск всех серверов

```bash
docker-compose up
```

Это запустит три сервера:
- **TCP сервер** - `localhost:8888`
- **WebSocket сервер** - `localhost:8889`
- **REST API сервер** - `localhost:8890`

### 2. Запуск в фоновом режиме

```bash
docker-compose up -d
```

### 3. Просмотр логов

```bash
# Все логи
docker-compose logs -f

# Логи конкретного сервера
docker-compose logs -f tcp-server
docker-compose logs -f websocket-server
docker-compose logs -f rest-server
```

## Подключение клиентов

### TCP клиент (интерактивный режим)

```bash
docker-compose run tcp-client
```

Затем введите nickname и команды:
```
Enter your nickname: Alice
> /join general
> Hello from Docker!
```

### WebSocket клиент (интерактивный режим)

```bash
docker-compose run ws-client
```

### REST API (из хост-системы)

```bash
# Join room
curl -X POST http://localhost:8890/api/join \
  -H "Content-Type: application/json" \
  -d '{"nickname":"charlie","room":"general"}'

# Send message
curl -X POST http://localhost:8890/api/send \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"<uuid>","text":"Hello from REST!"}'

# Poll messages
curl "http://localhost:8890/api/messages?sessionId=<uuid>"
```

## Управление контейнерами

### Остановка

```bash
# Остановить все контейнеры
docker-compose down

# Остановить и удалить volumes
docker-compose down -v
```

### Перезапуск

```bash
# Перезапустить все
docker-compose restart

# Перезапустить конкретный сервер
docker-compose restart tcp-server
```

### Пересборка

После изменения кода:

```bash
# Пересобрать образы
docker-compose build

# Пересобрать и запустить
docker-compose up --build
```

## Проверка состояния

```bash
# Статус контейнеров
docker-compose ps

# Ресурсы
docker stats

# Сеть
docker network ls
docker network inspect untitled_chat-network
```

## Docker архитектура

```
┌─────────────────────────────────────────┐
│         Docker Compose Setup            │
└─────────────────────────────────────────┘

┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  tcp-server      │  │  websocket-srv   │  │  rest-server     │
│  :8888           │  │  :8889           │  │  :8890           │
│  (always up)     │  │  (always up)     │  │  (always up)     │
└────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘
         │                     │                      │
         └─────────────────────┴──────────────────────┘
                               │
                     chat-network (bridge)
                               │
         ┌─────────────────────┴──────────────────────┐
         │                                             │
┌────────┴─────────┐                      ┌───────────┴──────┐
│  tcp-client      │                      │  ws-client       │
│  (on demand)     │                      │  (on demand)     │
│  profile: client │                      │  profile: client │
└──────────────────┘                      └──────────────────┘
```

## Profiles

Docker Compose использует profiles для разделения серверов и клиентов:

- **Серверы**: запускаются автоматически с `docker-compose up`
- **Клиенты**: запускаются вручную с `docker-compose run`

Клиенты в профиле `client` не стартуют автоматически, потому что:
1. Они интерактивные (требуют ввода пользователя)
2. Обычно нужны для тестирования, а не постоянной работы
3. Можно запустить несколько клиентов одновременно

## Multi-stage build

Dockerfile использует multi-stage build для оптимизации:

### Stage 1: Build
```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
# Собирает JAR файл
```

### Stage 2: Runtime
```dockerfile
FROM eclipse-temurin:21-jre-jammy
# Только JRE + собранный JAR
# Меньший размер образа
```

Преимущества:
- **Меньший размер**: финальный образ содержит только JRE и JAR
- **Безопасность**: нет build tools в production образе
- **Кэширование**: зависимости Maven кэшируются отдельно

## Переменные окружения

Можно настроить через `docker-compose.yml`:

```yaml
services:
  tcp-server:
    environment:
      - JAVA_OPTS=-Xmx512m
      - SERVER_PORT=8888
```

## Volumes (опционально)

Для сохранения данных между перезапусками:

```yaml
services:
  tcp-server:
    volumes:
      - chat-data:/app/data

volumes:
  chat-data:
```

## Troubleshooting

### Порт уже занят

```bash
# Проверить что использует порт
lsof -i :8888

# Или изменить порт в docker-compose.yml
ports:
  - "9888:8888"  # хост:контейнер
```

### Не могу подключиться к серверу

```bash
# Проверить что контейнер запущен
docker-compose ps

# Проверить логи
docker-compose logs tcp-server

# Проверить сеть
docker network inspect untitled_chat-network
```

### Образ не пересобирается

```bash
# Принудительная пересборка
docker-compose build --no-cache

# Удалить все и пересобрать
docker-compose down
docker-compose build --no-cache
docker-compose up
```

### Out of memory

```bash
# Увеличить лимиты в docker-compose.yml
services:
  tcp-server:
    deploy:
      resources:
        limits:
          memory: 1G
```

## Production deployment

Для production рекомендуется:

1. Использовать health checks:
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8890/health"]
  interval: 30s
  timeout: 10s
  retries: 3
```

2. Настроить restart policy:
```yaml
restart: unless-stopped
```

3. Использовать Docker secrets для конфигурации

4. Настроить logging driver:
```yaml
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

## Альтернативные способы запуска

### Только Docker (без Compose)

```bash
# Сборка
docker build -t chat-server .

# Создать сеть
docker network create chat-network

# Запуск серверов
docker run -d --name tcp-server --network chat-network \
  -p 8888:8888 chat-server \
  java -cp /app/app.jar org.example.Main tcp-server 8888

docker run -d --name ws-server --network chat-network \
  -p 8889:8889 chat-server \
  java -cp /app/app.jar org.example.Main ws-server 8889

docker run -d --name rest-server --network chat-network \
  -p 8890:8890 chat-server \
  java -cp /app/app.jar org.example.Main rest-server 8890
```

## См. также

- [README.md](README.md) - Общее описание проекта
- [scripts/README.md](scripts/README.md) - Документация по скриптам
- [forme/](forme/) - Детальная техническая документация