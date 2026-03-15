# Multi-Protocol Chat System

Многопротокольная система чата на Java 21 с поддержкой TCP, WebSocket и REST API.

## Особенности

Три протокола**: TCP (8888), WebSocket (8889), REST (8890)


## Быстрый старт

> **Требования:** Java 21, Maven 3 (или Docker + Docker Compose)

### 1 — Сборка проекта

```bash
mvn clean package -DskipTests
```

### 2 — Запуск сервера

```bash
./server.sh
```

Выберите вариант:
- `1` - TCP сервер (порт 8888)
- `2` - WebSocket сервер (порт 8889)
- `3` - REST API сервер (порт 8890)
- `4` - Все серверы в Docker
- `5` - Все серверы локально (в фоне)

### 3 — Подключение клиента

Откройте новый терминал и запустите:

```bash
./client.sh
```

Выберите протокол:
- `1` - TCP клиент
- `2` - WebSocket клиент
- `3` - REST API клиент (интерактивный)

Откройте дополнительные терминалы и снова запустите `./client.sh` для подключения нескольких пользователей.

### Остановка серверов

```bash
# Если запущены локально в фоне (опция 5)
pkill -f "org.example"

# Если запущены в Docker (опция 4)
docker-compose down
```

## Использование

### Команды в чате

| Команда | Описание |
|---------|----------|
| `/join <room>` | Войти в комнату |
| `/rooms` | Показать все активные комнаты |
| `/users` | Показать всех активных пользователей |
| `/history` | Показать последние 20 сообщений комнаты |
| `/help` | Показать справку по командам |
| `/quit` | Выйти из чата |

### Пример сессии

```bash
$ ./client.sh
Select protocol:
  1 - TCP (port 8888)
  2 - WebSocket (port 8889)
  3 - REST API (port 8890)

Enter choice [1-3]: 1

Connected to server at localhost:8888
Enter your nickname: Alice
Welcome Alice!
Type /join <room> to join a room

> /join general
Joined room: general

> Hello everyone!

> /rooms
=== Active Rooms ===
  general (3 users)
  random (1 user)
  lobby (5 users)
====================

> /users
=== Active Users ===
  - Alice
  - Bob
  - Charlie
  - David
====================

> /quit
Disconnecting...
```

### REST API

Используйте `./client.sh` и выберите опцию 3 для интерактивного REST клиента.

Или напрямую через curl:

```bash
# Join
curl -X POST http://localhost:8890/api/join \
  -H "Content-Type: application/json" \
  -d '{"nickname":"Alice","room":"general"}'

# Send message
curl -X POST http://localhost:8890/api/send \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"SESSION_ID","text":"Hello"}'

# Get messages (long polling, 30 sec)
curl "http://localhost:8890/api/messages?sessionId=SESSION_ID"

# Get rooms
curl "http://localhost:8890/api/rooms?sessionId=SESSION_ID"

# Get users
curl "http://localhost:8890/api/users?sessionId=SESSION_ID"

# Get history
curl "http://localhost:8890/api/history?sessionId=SESSION_ID"
```

### WebSocket HTML Тестер

Для визуального тестирования WebSocket откройте в браузере:

```bash
open websocket-test.html
```

Или перетащите файл `websocket-test.html` в браузер.

**Возможности:**
- Подключение к WebSocket серверу
- Визуальный интерфейс
- Отправка и получение сообщений в реальном времени
- Управление комнатами
- История сообщений

## Архитектура

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ TCP Server   │  │ WS Server    │  │ REST Server  │
│ Port 8888    │  │ Port 8889    │  │ Port 8890    │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                  │
       └─────────────────┴──────────────────┘
                         │
                         ▼
                ┌─────────────────┐
                │   RoomManager   │ ← Единый для всех
                │                 │
                │ MessageSender   │ ← Абстракция
                └─────────────────┘
```

Все протоколы используют:
- **RoomManager** - управление комнатами и broadcast
- **MessageSender** - абстракция отправки (TCP/WS push, REST queue)

## Структура проекта

```
untitled/
├── src/main/java/org/example/
│   ├── protocol/          # MessageSender, Message, MessageType
│   ├── server/
│   │   ├── RoomManager.java
│   │   ├── model/         # User, Room
│   │   ├── tcp/           # TCP сервер/клиент
│   │   ├── websocket/     # WebSocket сервер/клиент
│   │   └── rest/          # REST API + SessionManager
│   └── client/
│       ├── tcp/           # TCP клиент
│       └── websocket/     # WebSocket клиент
├── scripts/               # Скрипты запуска
├── websocket-test.html    # HTML тестер для WebSocket
├── Dockerfile
├── docker-compose.yml
└── DOCKER.md              # Docker документация
```

## Альтернативные способы запуска

### Использование отдельных скриптов

Если вы предпочитаете запускать серверы и клиенты напрямую:

```bash
# Запуск серверов (в отдельных терминалах)
./scripts/start-server.sh       # TCP сервер
./scripts/start-ws-server.sh    # WebSocket сервер
./scripts/start-rest-server.sh  # REST API сервер

# Запуск клиентов
./scripts/start-client.sh localhost 8888      # TCP клиент
./scripts/start-ws-client.sh localhost 8889   # WebSocket клиент
```

### С Docker Compose

```bash
# Запустить все серверы
docker-compose up

# Подключиться через клиенты (в отдельных терминалах)
docker-compose run tcp-client
docker-compose run ws-client
```

## Тестирование

### WebSocket тестер (браузер)
```bash
open websocket-test.html
```
Визуальный интерфейс для тестирования WebSocket в реальном времени.

### REST API тесты
Примеры curl команд см. в разделе "REST API" выше.

## Остановка серверов

```bash
pkill -f "org.example"

docker-compose down
```

**Порт занят:**
```bash
lsof -i :8888
lsof -i :8889
lsof -i :8890

pkill -f ChatServer
```
