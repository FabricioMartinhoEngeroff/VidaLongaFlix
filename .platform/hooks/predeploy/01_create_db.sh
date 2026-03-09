#!/bin/bash
# Garante que o banco "vidalongaflix" existe antes de subir o container.
# Roda na instância EC2 do EB antes de cada deploy.

set -e

# Extrai host, porta e nome do banco do DB_URL (ex: jdbc:postgresql://host:5432/vidalongaflix)
DB_URL="${DB_URL:-}"
DB_USERNAME="${DB_USERNAME:-}"
DB_PASSWORD="${DB_PASSWORD:-}"

if [ -z "$DB_URL" ]; then
  echo "[predeploy] DB_URL não definido, pulando criação do banco."
  exit 0
fi

# Extrai host, porta e nome do banco do JDBC URL
DB_HOST=$(echo "$DB_URL" | sed -E 's|jdbc:postgresql://([^:/]+).*|\1|')
DB_PORT=$(echo "$DB_URL" | sed -E 's|jdbc:postgresql://[^:]+:([0-9]+).*|\1|')
DB_PORT="${DB_PORT:-5432}"
DB_NAME=$(echo "$DB_URL" | sed -E 's|jdbc:postgresql://[^/]+/([^?]+).*|\1|')

if [ -z "$DB_NAME" ]; then
  echo "[predeploy] Não foi possível extrair o nome do banco de DB_URL, pulando criação."
  exit 0
fi

echo "[predeploy] Verificando banco de dados '$DB_NAME' em $DB_HOST:$DB_PORT..."

DB_EXISTS=$(
  PGPASSWORD="$DB_PASSWORD" psql \
    -h "$DB_HOST" \
    -p "$DB_PORT" \
    -U "$DB_USERNAME" \
    -d postgres \
    -tAc "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME';" 2>/dev/null || true
)

if [ "$DB_EXISTS" = "1" ]; then
  echo "[predeploy] Banco '$DB_NAME' já existe."
else
  PGPASSWORD="$DB_PASSWORD" psql \
    -h "$DB_HOST" \
    -p "$DB_PORT" \
    -U "$DB_USERNAME" \
    -d postgres \
    -c "CREATE DATABASE \"$DB_NAME\";" 2>/dev/null || echo "[predeploy] Sem permissão para criar '$DB_NAME' ou ocorreu outro erro — continuando."
fi

echo "[predeploy] Pronto."
