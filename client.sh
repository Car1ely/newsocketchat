#!/bin/bash
set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

HOST="${1:-localhost}"

echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║  Multi-Protocol Chat Client Launcher  ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Select protocol:${NC}"
echo -e "  ${YELLOW}1${NC} - TCP (port 8888)"
echo -e "  ${YELLOW}2${NC} - WebSocket (port 8889)"
echo -e "  ${YELLOW}3${NC} - REST API (port 8890)"
echo ""
read -p "Enter choice [1-3]: " choice

case $choice in
    1)
        echo -e "\n${GREEN}Starting TCP client...${NC}"
        PORT=8888
        java -cp target/untitled-1.0-SNAPSHOT.jar org.example.Main client "$HOST" "$PORT"
        ;;
    2)
        echo -e "\n${GREEN}Starting WebSocket client...${NC}"
        PORT=8889
        java -cp target/untitled-1.0-SNAPSHOT.jar org.example.client.websocket.WebSocketClient "$HOST" "$PORT"
        ;;
    3)
        echo -e "\n${GREEN}Starting REST API interactive client...${NC}"
        PORT=8890

        echo -e "${BLUE}Enter your nickname:${NC}"
        read -r NICKNAME

        echo -e "${BLUE}Enter room name:${NC}"
        read -r ROOM

        echo -e "\n${YELLOW}Connecting to REST API server at $HOST:$PORT...${NC}\n"

        RESPONSE=$(curl -s -X POST "http://$HOST:$PORT/api/join" \
            -H "Content-Type: application/json" \
            -d "{\"nickname\":\"$NICKNAME\",\"room\":\"$ROOM\"}")

        echo "Server response: $RESPONSE"

        SESSION_ID=$(echo "$RESPONSE" | grep -o '"sessionId":"[^"]*"' | cut -d'"' -f4)

        if [ -z "$SESSION_ID" ]; then
            echo -e "${RED}Failed to connect. Check if server is running.${NC}"
            exit 1
        fi

        echo -e "${GREEN}Connected! Session ID: $SESSION_ID${NC}\n"
        (
            while true; do
                MESSAGES=$(curl -s "http://$HOST:$PORT/api/messages?sessionId=$SESSION_ID" 2>/dev/null || echo "")
                if [ ! -z "$MESSAGES" ] && [ "$MESSAGES" != '{"success":true,"messages":[]}' ]; then
                    echo "$MESSAGES" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if data.get('success') and data.get('messages'):
        for msg in data['messages']:
            if msg.get('type') == 'BROADCAST':
                sender = msg.get('params', [''])[0]
                text = msg.get('params', ['', ''])[1] if len(msg.get('params', [])) > 1 else ''
                print(f'[{sender}]: {text}')
            elif msg.get('type') == 'USER_JOINED':
                user = msg.get('params', [''])[0]
                print(f'* {user} joined the room')
            elif msg.get('type') == 'USER_LEFT':
                user = msg.get('params', [''])[0]
                print(f'* {user} left the room')
except:
    pass
" 2>/dev/null
                fi
                sleep 1
            done
        ) &
        POLL_PID=$!

        echo -e "${BLUE}Available commands:${NC}"
        echo "  /rooms   - Show all active rooms"
        echo "  /users   - Show all active users"
        echo "  /history - Show last 20 messages"
        echo "  /quit    - Disconnect and exit"
        echo ""
        echo "Type your message and press Enter to send:"
        echo ""

        trap "kill $POLL_PID 2>/dev/null; exit" INT TERM

        while true; do
            read -r INPUT

            if [ -z "$INPUT" ]; then
                continue
            fi

            if [ "$INPUT" = "/quit" ] || [ "$INPUT" = "/exit" ]; then
                kill $POLL_PID 2>/dev/null
                echo -e "\n${YELLOW}Disconnecting...${NC}"
                exit 0
            elif [ "$INPUT" = "/rooms" ]; then
                echo -e "\n${BLUE}Active Rooms:${NC}"
                curl -s "http://$HOST:$PORT/api/rooms?sessionId=$SESSION_ID" | python3 -c "
import sys, json
data = json.load(sys.stdin)
if data.get('success'):
    for room in data.get('rooms', []):
        print(f\"  {room['name']} ({room['userCount']} users)\")
" 2>/dev/null
                echo ""
            elif [ "$INPUT" = "/users" ]; then
                echo -e "\n${BLUE}Active Users:${NC}"
                curl -s "http://$HOST:$PORT/api/users?sessionId=$SESSION_ID" | python3 -c "
import sys, json
data = json.load(sys.stdin)
if data.get('success'):
    for user in data.get('users', []):
        print(f\"  - {user}\")
" 2>/dev/null
                echo ""
            elif [ "$INPUT" = "/history" ]; then
                echo -e "\n${BLUE}Message History:${NC}"
                curl -s "http://$HOST:$PORT/api/history?sessionId=$SESSION_ID" | python3 -c "
import sys, json
data = json.load(sys.stdin)
if data.get('success'):
    if data.get('history'):
        for msg in data.get('history', []):
            print(msg)
    else:
        print('  No messages yet')
" 2>/dev/null
                echo ""
            else
                curl -s -X POST "http://$HOST:$PORT/api/send" \
                    -H "Content-Type: application/json" \
                    -d "{\"sessionId\":\"$SESSION_ID\",\"text\":\"$INPUT\"}" > /dev/null
            fi
        done
        ;;
    *)
        echo -e "${RED}Invalid choice. Please run again and select 1, 2, or 3.${NC}"
        exit 1
        ;;
esac
