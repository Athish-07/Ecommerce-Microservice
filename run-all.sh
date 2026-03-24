#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOGS_DIR="$ROOT/logs"
SKIP_BUILD=false

test_port_open() {
  local host="$1"
  local port="$2"
  python - "$host" "$port" <<'PY'
import socket
import sys
host = sys.argv[1]
port = int(sys.argv[2])
sock = socket.socket()
sock.settimeout(2)
try:
    sock.connect((host, port))
    print("open")
except OSError:
    print("closed")
finally:
    sock.close()
PY
}

docker_compose_cmd() {
  if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    echo "docker compose"
    return 0
  fi

  if command -v docker-compose >/dev/null 2>&1; then
    echo "docker-compose"
    return 0
  fi

  return 1
}

start_rabbitmq() {
  if [[ "$(test_port_open localhost 5672)" == "open" ]]; then
    echo "RabbitMQ is already reachable on port 5672."
    return 0
  fi

  local compose_cmd
  compose_cmd="$(docker_compose_cmd || true)"
  if [[ -z "$compose_cmd" ]]; then
    echo "RabbitMQ is not reachable on port 5672 and Docker Compose was not found." >&2
    exit 1
  fi

  echo "Starting RabbitMQ with Docker Compose..."
  # shellcheck disable=SC2086
  $compose_cmd -f "$ROOT/docker-compose.yml" up -d rabbitmq

  local timeout_seconds=90
  local start_time
  start_time="$(date +%s)"
  while true; do
    if [[ "$(test_port_open localhost 5672)" == "open" ]]; then
      echo "RabbitMQ is UP"
      return 0
    fi

    if (( "$(date +%s)" - start_time >= timeout_seconds )); then
      echo "Timed out waiting for RabbitMQ on localhost:5672" >&2
      exit 1
    fi

    sleep 3
  done
}

if [[ "${1:-}" == "--skip-build" ]]; then
  SKIP_BUILD=true
fi

if [[ -f "$ROOT/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/.env"
  set +a
fi

require_env() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: $name" >&2
    exit 1
  fi
}

wait_for_health() {
  local name="$1"
  local url="$2"
  local timeout_seconds=120
  local start_time
  start_time="$(date +%s)"

  while true; do
    if curl -fsS "$url" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then
      echo "$name is UP"
      return 0
    fi

    if (( "$(date +%s)" - start_time >= timeout_seconds )); then
      echo "Timed out waiting for $name health at $url" >&2
      return 1
    fi

    sleep 3
  done
}

start_service() {
  local name="$1"
  local module="$2"
  local health_url="$3"
  local module_path="$ROOT/$module"
  local out_log="$LOGS_DIR/$name.out.log"
  local err_log="$LOGS_DIR/$name.err.log"

  echo "Starting $name..."
  (
    cd "$module_path"
    "$MVN_CMD" spring-boot:run
  ) >"$out_log" 2>"$err_log" &

  wait_for_health "$name" "$health_url"
}

if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "JAVA_HOME is not set." >&2
  exit 1
fi

if [[ -n "${MAVEN_HOME:-}" ]]; then
  MVN_CMD="$MAVEN_HOME/bin/mvn"
else
  MVN_CMD="$(command -v mvn || true)"
fi

if [[ -z "${MVN_CMD:-}" || ! -x "$MVN_CMD" ]]; then
  echo "Maven was not found. Set MAVEN_HOME or add mvn to PATH." >&2
  exit 1
fi

export PATH="$JAVA_HOME/bin${PATH:+:$PATH}"
if [[ -n "${MAVEN_HOME:-}" ]]; then
  export PATH="$MAVEN_HOME/bin:$PATH"
fi

require_env JWT_PRIVATE_KEY
require_env JWT_PUBLIC_KEY
require_env AUTH_DB_PASSWORD
require_env PRODUCT_DB_PASSWORD
require_env INVENTORY_DB_PASSWORD
require_env ORDER_DB_PASSWORD

mkdir -p "$LOGS_DIR"
start_rabbitmq

if [[ "$SKIP_BUILD" != true ]]; then
  echo "Building all modules from the root aggregator pom..."
  "$MVN_CMD" -f "$ROOT/pom.xml" clean install -DskipTests
fi

start_service "config-server" "config-server" "http://localhost:8888/actuator/health"
start_service "service-registry" "service-registry" "http://localhost:8761/actuator/health"
start_service "auth-service" "auth-service" "http://localhost:8081/actuator/health"
start_service "product-service" "product-service" "http://localhost:8082/actuator/health"
start_service "inventory-service" "inventory-service" "http://localhost:8084/actuator/health"
start_service "order-service" "order-service" "http://localhost:8083/actuator/health"
start_service "api-gateway" "api-gateway" "http://localhost:8080/actuator/health"

echo
echo "All services are running."
echo "Logs directory: $LOGS_DIR"
