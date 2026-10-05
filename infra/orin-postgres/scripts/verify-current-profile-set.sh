#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
VERIFY_SQL="${SCRIPT_DIR}/../verification/90-verify-face-profiles.sql"

command -v psql >/dev/null 2>&1 || {
    echo "Verification refused: psql is not available." >&2
    exit 69
}

if [[ ! -r "${VERIFY_SQL}" ]]; then
    echo "Verification refused: verification SQL is missing or unreadable." >&2
    exit 66
fi

export PGDATABASE="${PGDATABASE:-${POSTGRES_DB:-}}"
export PGUSER="${PGUSER:-${POSTGRES_USER:-}}"
export PGPASSWORD="${PGPASSWORD:-${POSTGRES_PASSWORD:-}}"
export PGHOST="${PGHOST:-127.0.0.1}"
export PGPORT="${PGPORT:-5432}"
export PGCONNECT_TIMEOUT="${PGCONNECT_TIMEOUT:-5}"

: "${PGDATABASE:?PGDATABASE or POSTGRES_DB is required}"
: "${PGUSER:?PGUSER or POSTGRES_USER is required}"

exec psql \
    --no-psqlrc \
    --set=ON_ERROR_STOP=1 \
    --file="${VERIFY_SQL}"
