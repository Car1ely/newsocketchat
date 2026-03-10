# Multi-Protocol Chat System

Многопротокольная система чата на Java 21 с поддержкой TCP, WebSocket и REST API.

## Особенности

Три протокола**: TCP (8888), WebSocket (8889), REST (8890)


## Быстрый старт

### С Docker (рекомендуется)

```bash
# Запустить все серверы
docker-compose up

# TCP клиент
docker-compose run tcp-client

# WebSocket клиент
docker-compose run ws-client
```

### Без Docker

```bash
# 1. Сборка
mvn clean package -DskipTests

# 2. Запуск серверов (в разных терминалах)
./scripts/start-server.sh      # TCP сервер
./scripts/start-ws-server.sh   # WebSocket сервер
./scripts/start-rest-server.sh # REST API сервер

# 3. Подключение клиентов (выберите один)

# TCP клиент
./scripts/start-client.sh localhost 8888

# WebSocket клиент
./scripts/start-ws-client.sh localhost 8889
```

## Использование

### TCP/WebSocket клиенты

```bash
Enter your nickname: Alice
> /join general          # Войти в комнату
> Hello everyone!        # Отправить сообщение
> /join random          # Переключить комнату
> /quit                 # Выйти
```

### REST API

**⚠️ ВАЖНО: Скопируйте ВЕСЬ блок ниже целиком!**

```bash
NICKNAME="User_$$_$RANDOM"
RESPONSE=$(curl -s -X POST http://localhost:8890/api/join -H "Content-Type: application/json" -d "{\"nickname\":\"$NICKNAME\",\"room\":\"general\"}")
echo "Response: $RESPONSE"
SESSION_ID=$(echo "$RESPONSE" | grep -o '"sessionId":"[^"]*"' | cut -d'"' -f4)
echo "Session ID: $SESSION_ID"
if [ -z "$SESSION_ID" ]; then echo "❌ ERROR: Session ID is empty! Check if server is running."; exit 1; fi
cat > /tmp/msg.json << EOF
{"sessionId":"$SESSION_ID","text":"Hello from REST!"}
EOF
echo "✅ Sending message..."
curl -s -X POST http://localhost:8890/api/send -H "Content-Type: application/json" -d @/tmp/msg.json
echo ""
echo "✅ Polling messages (wait up to 30 sec)..."
curl -s "http://localhost:8890/api/messages?sessionId=$SESSION_ID"
```

**Или пошагово (если нужно делать руками):**

1. **Join и получить sessionId:**
```bash
curl -s -X POST http://localhost:8890/api/join -H "Content-Type: application/json" -d '{"nickname":"Alice","room":"general"}'
```
Скопируйте `sessionId` из ответа (UUID формат).

2. **Отправить сообщение:**
```bash
curl -s -X POST http://localhost:8890/api/send -H "Content-Type: application/json" -d '{"sessionId":"ВСТАВЬТЕ_sessionId","text":"Hello!"}'
```

3. **Получить сообщения (long polling 30 сек):**
```bash
curl -s "http://localhost:8890/api/messages?sessionId=ВСТАВЬТЕ_sessionId"
```

### WebSocket HTML Тестер

Для визуального тестирования WebSocket откройте в браузере:

```bash
open websocket-test.html
```

Или перетащите файл `websocket-test.html` в браузер.

**Возможности:**
- ✅ Подключение к WebSocket серверу
- ✅ Визуальный интерфейс
- ✅ Отправка и получение сообщений в реальном времени
- ✅ Управление комнатами
- ✅ История сообщений

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

## Скрипты

- `scripts/start-server.sh` - запуск TCP сервера
- `scripts/start-ws-server.sh` - запуск WebSocket сервера
- `scripts/start-rest-server.sh` - запуск REST API сервера
- `scripts/start-client.sh` - запуск TCP клиента
- `scripts/start-ws-client.sh` - запуск WebSocket клиента

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
