#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if ! command -v docker >/dev/null 2>&1; then
  echo "Error: Docker no está instalado o no está en el PATH." >&2
  exit 1
fi
if ! docker compose version >/dev/null 2>&1; then
  echo "Error: se requiere Docker Compose v2 (docker compose version)." >&2
  exit 1
fi

if [ ! -f .env ]; then
  cp .env.example .env
  echo "Creado .env desde .env.example"
fi

leer_env() {
  grep -E "^[[:space:]]*$1=" .env 2>/dev/null | tail -n1 | cut -d= -f2- | tr -d '\r' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
}

generar_secreto() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 32
  elif command -v python3 >/dev/null 2>&1; then
    python3 -c "import secrets; print(secrets.token_hex(32))"
  else
    od -vN 32 -An -tx1 /dev/urandom | tr -d ' \n'
    echo
  fi
}

JWT_ACTUAL="$(leer_env JWT_SECRET || true)"
if [ -z "${JWT_ACTUAL:-}" ] || [ "$JWT_ACTUAL" = "cambiar-por-un-secreto-largo" ] || [ "${#JWT_ACTUAL}" -lt 32 ]; then
  NUEVO="$(generar_secreto)"
  sed -i.bak "s|^JWT_SECRET=.*|JWT_SECRET=$NUEVO|" .env && rm -f .env.bak
  if [ -z "$(leer_env JWT_SECRET || true)" ]; then
    echo "JWT_SECRET=$NUEVO" >> .env
  fi
  echo "Generado nuevo JWT_SECRET en .env"
fi

PASS_ACTUAL="$(leer_env POSTGRES_PASSWORD || true)"
if [ "$PASS_ACTUAL" = "cambiar-esta-clave" ]; then
  echo "Aviso: POSTGRES_PASSWORD sigue con el valor de ejemplo. Cámbiala en .env para uso real."
fi

docker compose up -d --build

echo ""
echo "Listo:"
echo "  App web:     http://localhost:5173"
echo "  API/Swagger: http://localhost:8080/swagger-ui/index.html"
echo "Ver estado: docker compose ps | docker compose logs -f"
