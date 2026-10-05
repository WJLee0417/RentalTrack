#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
VERIFY_SCRIPT="${SCRIPT_DIR}/verify-current-profile-set.sh"

usage() {
    echo "Usage: $0 /absolute/path/to/face-profile.dump" >&2
}

if [[ $# -ne 1 ]]; then
    usage
    exit 64
fi

DUMP_FILE="$1"

if [[ "${DUMP_FILE}" != /* ]]; then
    echo "Restore refused: provide an absolute external dump path." >&2
    exit 64
fi

if [[ ! -f "${DUMP_FILE}" ]]; then
    echo "Restore refused: dump file does not exist or is not a regular file." >&2
    exit 66
fi

if [[ ! -r "${DUMP_FILE}" ]]; then
    echo "Restore refused: dump file is not readable." >&2
    exit 66
fi

command -v psql >/dev/null 2>&1 || {
    echo "Restore refused: psql is not available." >&2
    exit 69
}

export PGDATABASE="${PGDATABASE:-${POSTGRES_DB:-}}"
export PGUSER="${PGUSER:-${POSTGRES_USER:-}}"
export PGPASSWORD="${PGPASSWORD:-${POSTGRES_PASSWORD:-}}"
export PGHOST="${PGHOST:-127.0.0.1}"
export PGPORT="${PGPORT:-5432}"
export PGCONNECT_TIMEOUT="${PGCONNECT_TIMEOUT:-5}"

: "${PGDATABASE:?PGDATABASE or POSTGRES_DB is required}"
: "${PGUSER:?PGUSER or POSTGRES_USER is required}"

if [[ ! -r "${VERIFY_SCRIPT}" ]]; then
    echo "Restore refused: verification script is missing or unreadable." >&2
    exit 69
fi

TABLE_NAME="$({
    psql \
        --no-psqlrc \
        --tuples-only \
        --no-align \
        --set=ON_ERROR_STOP=1 \
        --command="SELECT COALESCE(to_regclass('public.face_profile')::text, '');"
} 2>/dev/null)" || {
    echo "Restore refused: target database is not reachable." >&2
    exit 69
}

if [[ "${TABLE_NAME}" != "face_profile" ]]; then
    echo "Restore refused: public.face_profile must be created before restoring data." >&2
    exit 65
fi

EXISTING_COUNT="$({
    psql \
        --no-psqlrc \
        --tuples-only \
        --no-align \
        --set=ON_ERROR_STOP=1 \
        --command="SELECT count(*) FROM public.face_profile;"
} 2>/dev/null)" || {
    echo "Restore refused: target profile count could not be read." >&2
    exit 69
}

if [[ "${EXISTING_COUNT}" != "0" ]]; then
    echo "Restore refused: target face_profile table is not empty." >&2
    exit 65
fi

if command -v pg_restore >/dev/null 2>&1 \
    && pg_restore --list "${DUMP_FILE}" >/dev/null 2>&1; then
    if ! pg_restore \
        --exit-on-error \
        --single-transaction \
        --data-only \
        --no-owner \
        --no-privileges \
        --dbname="${PGDATABASE}" \
        "${DUMP_FILE}" >/dev/null 2>&1; then
        echo "Restore failed. Dump contents were suppressed from the log." >&2
        exit 1
    fi
else
    if ! psql \
        --no-psqlrc \
        --single-transaction \
        --set=ON_ERROR_STOP=1 \
        --file="${DUMP_FILE}" >/dev/null 2>&1; then
        echo "Restore failed. Dump contents were suppressed from the log." >&2
        exit 1
    fi
fi

echo "Restore completed; running metadata-only verification."
bash "${VERIFY_SCRIPT}"
