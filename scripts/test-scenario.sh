#!/bin/bash
# Automated test scenario for TCP Chat

# Найти корневую директорию проекта
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT" || exit 1

JAR_FILE="target/untitled-1.0-SNAPSHOT.jar"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Please run: mvn clean package -DskipTests"
    exit 1
fi

echo "=== TCP Chat Test Scenario ==="
echo ""
echo "Starting 3 clients with automated commands..."
echo ""

# Client 1: Alice in room 'general'
(
  echo "Alice"
  sleep 1
  echo "/join general"
  sleep 2
  echo "Hello everyone!"
  sleep 2
  echo "I'm Alice from general room"
  sleep 3
  echo "/quit"
) | java -cp "$JAR_FILE" org.example.Main client localhost 8888 > /tmp/alice.log 2>&1 &
ALICE_PID=$!
echo "Client 1 (Alice) started with PID $ALICE_PID"

sleep 1

# Client 2: Bob in room 'general'
(
  echo "Bob"
  sleep 1
  echo "/join general"
  sleep 2
  echo "Hi Alice!"
  sleep 2
  echo "Bob here!"
  sleep 3
  echo "/quit"
) | java -cp "$JAR_FILE" org.example.Main client localhost 8888 > /tmp/bob.log 2>&1 &
BOB_PID=$!
echo "Client 2 (Bob) started with PID $BOB_PID"

sleep 1

# Client 3: Charlie in room 'random'
(
  echo "Charlie"
  sleep 1
  echo "/join random"
  sleep 2
  echo "Anyone in random room?"
  sleep 2
  echo "I'm all alone here..."
  sleep 3
  echo "/quit"
) | java -cp "$JAR_FILE" org.example.Main client localhost 8888 > /tmp/charlie.log 2>&1 &
CHARLIE_PID=$!
echo "Client 3 (Charlie) started with PID $CHARLIE_PID"

echo ""
echo "All clients started. Waiting for test to complete..."
sleep 8

echo ""
echo "=== Test Results ==="
echo ""
echo "--- Alice's session (general room) ---"
cat /tmp/alice.log
echo ""
echo "--- Bob's session (general room) ---"
cat /tmp/bob.log
echo ""
echo "--- Charlie's session (random room) ---"
cat /tmp/charlie.log
echo ""
echo "=== Expected behavior ==="
echo "✓ Alice and Bob should see each other's messages"
echo "✓ Charlie should NOT see Alice/Bob messages (different room)"
echo "✓ Alice/Bob should see USER_JOINED notifications"
echo "✓ No crashes or errors"
