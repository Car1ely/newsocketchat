#!/bin/bash
set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║  Multi-Protocol Chat Server Launcher  ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Select server to start:${NC}"
echo -e "  ${YELLOW}1${NC} - TCP Server (port 8888)"
echo -e "  ${YELLOW}2${NC} - WebSocket Server (port 8889)"
echo -e "  ${YELLOW}3${NC} - REST API Server (port 8890)"
echo -e "  ${YELLOW}4${NC} - All servers (Docker Compose)"
echo -e "  ${YELLOW}5${NC} - All servers (local, background)"
echo ""
read -p "Enter choice [1-5]: " choice

case $choice in
    1)
        echo -e "\n${GREEN}Starting TCP Server on port 8888...${NC}\n"
        java -cp target/untitled-1.0-SNAPSHOT.jar org.example.Main server 8888
        ;;
    2)
        echo -e "\n${GREEN}Starting WebSocket Server on port 8889...${NC}\n"
        java -cp target/untitled-1.0-SNAPSHOT.jar org.example.server.websocket.WebSocketServer 8889
        ;;
    3)
        echo -e "\n${GREEN}Starting REST API Server on port 8890...${NC}\n"
        java -cp target/untitled-1.0-SNAPSHOT.jar org.example.Main rest 8890
        ;;
    4)
        echo -e "\n${GREEN}Starting all servers with Docker Compose...${NC}\n"
        docker-compose up --build
        ;;
    5)
        echo -e "\n${GREEN}Starting all servers in background...${NC}\n"

        echo -e "${BLUE}Starting TCP Server (8888)...${NC}"
        java -cp target/untitled-1.0-SNAPSHOT.jar org.example.Main server 8888 > tcp-server.log 2>&1 &
        TCP_PID=$!
        echo "  PID: $TCP_PID"

        sleep 1

        echo -e "${BLUE}Starting WebSocket Server (8889)...${NC}"
        java -cp target/untitled-1.0-SNAPSHOT.jar org.example.server.websocket.WebSocketServer 8889 > ws-server.log 2>&1 &
        WS_PID=$!
        echo "  PID: $WS_PID"

        sleep 1

        echo -e "${BLUE}Starting REST API Server (8890)...${NC}"
        java -cp target/untitled-1.0-SNAPSHOT.jar org.example.Main rest 8890 > rest-server.log 2>&1 &
        REST_PID=$!
        echo "  PID: $REST_PID"

        sleep 2

        echo -e "\n${GREEN}All servers started!${NC}\n"
        echo "Logs:"
        echo "  TCP:       tail -f tcp-server.log"
        echo "  WebSocket: tail -f ws-server.log"
        echo "  REST API:  tail -f rest-server.log"
        echo ""
        echo "To stop all servers:"
        echo "  kill $TCP_PID $WS_PID $REST_PID"
        echo "  or: pkill -f \"org.example\""
        echo ""

        echo "$TCP_PID $WS_PID $REST_PID" > .server-pids
        echo -e "${YELLOW}Server PIDs saved to .server-pids${NC}"
        ;;
    *)
        echo -e "${RED}Invalid choice. Please run again and select 1-5.${NC}"
        exit 1
        ;;
esac
