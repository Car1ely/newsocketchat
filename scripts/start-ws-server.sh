#!/bin/bash
# Start WebSocket Chat Server

# Найти корневую директорию проекта
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT" || exit 1

echo "Starting WebSocket Chat Server on port ${1:-8889}..."
java -cp target/untitled-1.0-SNAPSHOT.jar org.example.server.websocket.WebSocketServer ${1:-8889}
