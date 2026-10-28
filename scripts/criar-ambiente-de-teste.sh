#!/usr/bin/env bash
# Cria o banco e o vhost do RabbitMQ usados SÓ pelos testes (./mvnw test), para que a suíte
# nunca toque nos dados de desenvolvimento nem no banco do E2E do front (shottrack_test).
# Idempotente: pode rodar quantas vezes quiser. Requer `docker compose up -d` rodando.
set -euo pipefail

TEST_DB="${TEST_DB:-shottrack_unit_test}"
TEST_VHOST="${TEST_VHOST:-shottrack_unit_test}"
SERVICE_USER="${SHOTTRACK_SERVICE_USER:-shottrack}"

cd "$(dirname "$0")/.."

if docker compose version >/dev/null 2>&1; then
  COMPOSE=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE=(docker-compose)
else
  echo "docker compose não encontrado" >&2
  exit 1
fi

echo "Postgres: garantindo o banco ${TEST_DB}..."
# `sh -c`: o docker-compose v1 interpretaria as flags do psql (-d, -U) como suas
psql_exec() { "${COMPOSE[@]}" exec -T postgres sh -c "PGUSER='${SERVICE_USER}' psql -d postgres -At -c \"$1\""; }
if [ "$(psql_exec "SELECT 1 FROM pg_database WHERE datname = '${TEST_DB}'")" != "1" ]; then
  psql_exec "CREATE DATABASE ${TEST_DB}"
else
  echo "  já existe."
fi

echo "RabbitMQ: garantindo o vhost ${TEST_VHOST}..."
if ! "${COMPOSE[@]}" exec -T rabbitmq rabbitmqctl list_vhosts -q | grep -qx "${TEST_VHOST}"; then
  "${COMPOSE[@]}" exec -T rabbitmq rabbitmqctl add_vhost "${TEST_VHOST}"
else
  echo "  já existe."
fi
"${COMPOSE[@]}" exec -T rabbitmq rabbitmqctl set_permissions -p "${TEST_VHOST}" "${SERVICE_USER}" ".*" ".*" ".*"

echo "Pronto. Rode: JWT_SECRET=\$(openssl rand -base64 32) ./mvnw test"
