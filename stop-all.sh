#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

stop_service() {
  local name="$1"
  local module="$2"
  local module_path="$ROOT/$module"

  local pids
  pids="$(pgrep -f "$module_path" || true)"

  if [[ -z "$pids" ]]; then
    echo "$name is not running"
    return 0
  fi

  echo "Stopping $name..."
  while read -r pid; do
    [[ -n "$pid" ]] && kill "$pid"
  done <<< "$pids"
}

stop_service "api-gateway" "api-gateway"
stop_service "order-service" "order-service"
stop_service "inventory-service" "inventory-service"
stop_service "product-service" "product-service"
stop_service "auth-service" "auth-service"
stop_service "service-registry" "service-registry"
stop_service "config-server" "config-server"

if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  docker compose -f "$ROOT/docker-compose.yml" stop rabbitmq >/dev/null 2>&1 || true
elif command -v docker-compose >/dev/null 2>&1; then
  docker-compose -f "$ROOT/docker-compose.yml" stop rabbitmq >/dev/null 2>&1 || true
fi

echo
echo "Stop requests sent."
