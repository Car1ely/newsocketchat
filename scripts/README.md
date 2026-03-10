# Scripts Directory

Скрипты для запуска серверов и клиентов чат-системы.

## Предварительные требования

Перед запуском скриптов необходимо собрать проект:

```bash
cd ..  # Если вы в папке scripts/
mvn clean package -DskipTests
```

## Запуск серверов

Все скрипты можно запускать из корня проекта или из папки `scripts/`.

### TCP сервер (порт 8888)

```bash
./scripts/start-server.sh [PORT]

# Примеры:
./scripts/start-server.sh        # Запуск на порту 8888
./scripts/start-server.sh 9000   # Запуск на порту 9000
```

### WebSocket сервер (порт 8889)

```bash
./scripts/start-ws-server.sh [PORT]

# Примеры:
./scripts/start-ws-server.sh       # Запуск на порту 8889
./scripts/start-ws-server.sh 9001  # Запуск на порту 9001
```

### REST API сервер (порт 8890)

```bash
./scripts/start-rest-server.sh [PORT]

# Примеры:
./scripts/start-rest-server.sh       # Запуск на порту 8890
./scripts/start-rest-server.sh 9002  # Запуск на порту 9002
```

## Запуск клиентов

### TCP клиент

```bash
./scripts/start-client.sh [HOST] [PORT]

# Примеры:
./scripts/start-client.sh                    # localhost:8888
./scripts/start-client.sh localhost 9000     # localhost:9000
./scripts/start-client.sh 192.168.1.100 8888 # удаленный сервер
```

### WebSocket клиент

```bash
./scripts/start-ws-client.sh [HOST] [PORT]

# Примеры:
./scripts/start-ws-client.sh                    # localhost:8889
./scripts/start-ws-client.sh localhost 9001     # localhost:9001
./scripts/start-ws-client.sh 192.168.1.100 8889 # удаленный сервер
```

## Тестовый сценарий

Автоматизированный скрипт для тестирования всех компонентов системы:

```bash
./scripts/test-scenario.sh
```

Этот скрипт:
- Проверяет что проект собран
- Запускает серверы
- Проверяет их доступность
- Выполняет базовые тесты

## Остановка серверов

```bash
# Остановить все Java процессы проекта
pkill -f "org.example"

# Остановить конкретный сервер
pkill -f "ChatServer"        # TCP
pkill -f "WebSocketServer"   # WebSocket
pkill -f "RestServer"        # REST

# Или просто Ctrl+C в терминале сервера
```

## Проверка портов

```bash
# Проверить занятые порты
lsof -i :8888  # TCP
lsof -i :8889  # WebSocket
lsof -i :8890  # REST

# Посмотреть все Java процессы
ps aux | grep "org.example"
```

## Примечания

- Все скрипты автоматически определяют корневую директорию проекта
- Можно запускать из любой директории
- Скрипты ожидают что JAR файл находится в `target/untitled-1.0-SNAPSHOT.jar`
- **Все серверы и клиенты используют собранный JAR файл** (включает все зависимости)
- Если получаете `NoClassDefFoundError`, убедитесь что проект собран: `mvn clean package -DskipTests`