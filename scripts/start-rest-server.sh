#!/bin/bash
# Start REST API Server

# Найти корневую директорию проекта
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT" || exit 1

PORT=${1:-8890}

echo "Starting REST API Server on port $PORT..."
java -cp target/untitled-1.0-SNAPSHOT.jar org.example.server.rest.RestServer $PORT
