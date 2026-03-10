#!/bin/bash
# Start WebSocket Chat Client

# Найти корневую директорию проекта
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT" || exit 1

HOST=${1:-localhost}
PORT=${2:-8889}

echo "Connecting to WebSocket Chat Server at $HOST:$PORT..."
java -cp target/untitled-1.0-SNAPSHOT.jar org.example.client.websocket.WebSocketClient $HOST $PORT
