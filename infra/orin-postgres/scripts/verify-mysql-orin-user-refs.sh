#!/usr/bin/env bash

set -Eeuo pipefail
set +x
umask 077

# Cross-DB registration authority contract:
# - MySQL: face_registered=TRUE for the seven fixed Fixture user IDs
# - Orin: row existence in public.face_profile (registration_status is forbidden)
#
# Required connection variables:
#   MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE, MYSQL_USERNAME, MYSQL_PASSWORD
#   ORIN_POSTGRES_HOST, ORIN_POSTGRES_PORT, ORIN_POSTGRES_DB,
#   ORIN_POSTGRES_USER, ORIN_POSTGRES_PASSWORD
# MYSQL_USER/MYSQL_DB and ORIN_POSTGRES_DATABASE/ORIN_POSTGRES_USERNAME are
# accepted as compatibility aliases. Passwords use process environment only.

for command_name in mysql psql sort comm mktemp awk wc grep; do
    command -v "${command_name}" >/dev/null 2>&1 || {
        echo "Cross verification refused: ${command_name} is not available." >&2
        exit 69
    }
done

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DATABASE="${MYSQL_DATABASE:-${MYSQL_DB:-}}"
MYSQL_USERNAME="${MYSQL_USERNAME:-${MYSQL_USER:-}}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
MYSQL_CONNECT_TIMEOUT="${MYSQL_CONNECT_TIMEOUT:-5}"

ORIN_POSTGRES_HOST="${ORIN_POSTGRES_HOST:-${PGHOST:-127.0.0.1}}"
ORIN_POSTGRES_PORT="${ORIN_POSTGRES_PORT:-${PGPORT:-5432}}"
ORIN_POSTGRES_DATABASE="${ORIN_POSTGRES_DB:-${ORIN_POSTGRES_DATABASE:-${PGDATABASE:-}}}"
ORIN_POSTGRES_USERNAME="${ORIN_POSTGRES_USER:-${ORIN_POSTGRES_USERNAME:-${PGUSER:-}}}"
ORIN_POSTGRES_PASSWORD="${ORIN_POSTGRES_PASSWORD:-${PGPASSWORD:-}}"
ORIN_POSTGRES_CONNECT_TIMEOUT="${ORIN_POSTGRES_CONNECT_TIMEOUT:-${PGCONNECT_TIMEOUT:-5}}"

: "${MYSQL_DATABASE:?MYSQL_DATABASE is required}"
: "${MYSQL_USERNAME:?MYSQL_USERNAME or MYSQL_USER is required}"
: "${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}"
: "${ORIN_POSTGRES_DATABASE:?ORIN_POSTGRES_DB or ORIN_POSTGRES_DATABASE is required}"
: "${ORIN_POSTGRES_USERNAME:?ORIN_POSTGRES_USER or ORIN_POSTGRES_USERNAME is required}"
: "${ORIN_POSTGRES_PASSWORD:?ORIN_POSTGRES_PASSWORD is required}"

TEMP_DIR="$(mktemp -d)"
cleanup() {
    unset MYSQL_PWD PGPASSWORD
    rm -rf -- "${TEMP_DIR}"
}
trap cleanup EXIT

MYSQL_RAW="${TEMP_DIR}/mysql-raw.txt"
MYSQL_REFS="${TEMP_DIR}/mysql-refs.txt"
ORIN_RAW="${TEMP_DIR}/orin-raw.txt"
ORIN_REFS="${TEMP_DIR}/orin-refs.txt"
EXPECTED_REFS="${TEMP_DIR}/expected-refs.txt"
MYSQL_ONLY="${TEMP_DIR}/mysql-only.txt"
ORIN_ONLY="${TEMP_DIR}/orin-only.txt"
MYSQL_MISSING="${TEMP_DIR}/mysql-missing.txt"
ORIN_MISSING="${TEMP_DIR}/orin-missing.txt"
MYSQL_UNEXPECTED="${TEMP_DIR}/mysql-unexpected.txt"
ORIN_UNEXPECTED="${TEMP_DIR}/orin-unexpected.txt"

printf '%s\n' \
    'f6f509d4-c001-446c-b6b7-537d65a093c3' \
    'f86a8791-c02b-4e50-b761-d9dd009909a9' \
    '0b8255a1-70e4-45af-80f2-b38e4164ea06' \
    '59b97d2b-a37d-4f75-bd33-4beb1bd3ac76' \
    'd8a6f8d6-3c43-4789-b980-a9f6918d53b8' \
    '3650e75c-ab2b-4455-8f00-c691fd2e66fb' \
    | sort >"${EXPECTED_REFS}"

MYSQL_QUERY="
SELECT LOWER(CAST(user_ref AS CHAR(36)))
FROM user_account
WHERE user_id IN (
    '202600001', '202600002', '202600003', '202600004',
    '202600005', '202600006', '202600007'
)
  AND face_registered = TRUE
ORDER BY user_ref;
"

# MYSQL_PWD avoids exposing the secret in argv. set +x above prevents shell
# tracing from printing assignments or expanded environment values.
export MYSQL_PWD="${MYSQL_PASSWORD}"
if ! mysql \
    --host="${MYSQL_HOST}" \
    --port="${MYSQL_PORT}" \
    --user="${MYSQL_USERNAME}" \
    --database="${MYSQL_DATABASE}" \
    --connect-timeout="${MYSQL_CONNECT_TIMEOUT}" \
    --batch \
    --skip-column-names \
    --raw \
    --execute="${MYSQL_QUERY}" >"${MYSQL_RAW}"; then
    echo "Cross verification failed: MySQL user_ref query failed." >&2
    exit 1
fi
unset MYSQL_PWD

# Orin intentionally has no registration_status column. A face_profile row is
# the registration authority, and only user_ref metadata is selected here.
export PGPASSWORD="${ORIN_POSTGRES_PASSWORD}"
if ! PGCONNECT_TIMEOUT="${ORIN_POSTGRES_CONNECT_TIMEOUT}" psql \
    --no-psqlrc \
    --host="${ORIN_POSTGRES_HOST}" \
    --port="${ORIN_POSTGRES_PORT}" \
    --username="${ORIN_POSTGRES_USERNAME}" \
    --dbname="${ORIN_POSTGRES_DATABASE}" \
    --tuples-only \
    --no-align \
    --set=ON_ERROR_STOP=1 \
    --command='SELECT lower(user_ref::text) FROM public.face_profile ORDER BY user_ref;' \
    >"${ORIN_RAW}"; then
    echo "Cross verification failed: Orin user_ref query failed." >&2
    exit 1
fi
unset PGPASSWORD

awk 'NF { print tolower($0) }' "${MYSQL_RAW}" | sort -u >"${MYSQL_REFS}"
awk 'NF { print tolower($0) }' "${ORIN_RAW}" | sort -u >"${ORIN_REFS}"

UUID_PATTERN='^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
for result_file in "${MYSQL_REFS}" "${ORIN_REFS}"; do
    while IFS= read -r user_ref; do
        if [[ ! "${user_ref}" =~ ${UUID_PATTERN} ]]; then
            echo "Cross verification failed: a database returned a non-canonical UUID." >&2
            exit 1
        fi
    done <"${result_file}"
done

comm -23 "${MYSQL_REFS}" "${ORIN_REFS}" >"${MYSQL_ONLY}"
comm -13 "${MYSQL_REFS}" "${ORIN_REFS}" >"${ORIN_ONLY}"
comm -23 "${EXPECTED_REFS}" "${MYSQL_REFS}" >"${MYSQL_MISSING}"
comm -23 "${EXPECTED_REFS}" "${ORIN_REFS}" >"${ORIN_MISSING}"
comm -13 "${EXPECTED_REFS}" "${MYSQL_REFS}" >"${MYSQL_UNEXPECTED}"
comm -13 "${EXPECTED_REFS}" "${ORIN_REFS}" >"${ORIN_UNEXPECTED}"

MYSQL_COUNT="$(wc -l <"${MYSQL_REFS}")"
ORIN_COUNT="$(wc -l <"${ORIN_REFS}")"
FACE_UNREGISTERED_USER_REF='bb14cac2-74d1-4b7f-a69f-aa88fcf8f55e'
FAILED=0

report_difference() {
    local label="$1"
    local difference_file="$2"

    if [[ -s "${difference_file}" ]]; then
        echo "${label}:" >&2
        while IFS= read -r user_ref; do
            echo "  - ${user_ref}" >&2
        done <"${difference_file}"
        FAILED=1
    fi
}

if [[ "${MYSQL_COUNT}" -ne 6 ]]; then
    echo "MySQL registered Fixture user count mismatch: expected 6, found ${MYSQL_COUNT}." >&2
    FAILED=1
fi

if [[ "${ORIN_COUNT}" -ne 6 ]]; then
    echo "Orin face profile count mismatch: expected 6, found ${ORIN_COUNT}." >&2
    FAILED=1
fi

report_difference 'MySQL only user_ref' "${MYSQL_ONLY}"
report_difference 'Orin only user_ref' "${ORIN_ONLY}"
report_difference 'Expected normal user_ref missing from MySQL' "${MYSQL_MISSING}"
report_difference 'Expected normal user_ref missing from Orin' "${ORIN_MISSING}"
report_difference 'Unexpected registered Fixture user_ref in MySQL' "${MYSQL_UNEXPECTED}"
report_difference 'Unexpected face profile user_ref in Orin' "${ORIN_UNEXPECTED}"

if grep -Fqx "${FACE_UNREGISTERED_USER_REF}" "${ORIN_REFS}"; then
    echo "Face-unregistered Fixture user_ref must not exist in Orin: ${FACE_UNREGISTERED_USER_REF}." >&2
    FAILED=1
fi

if [[ "${FAILED}" -ne 0 ]]; then
    echo 'MySQL-Orin user_ref cross verification FAILED.' >&2
    exit 1
fi

echo 'MySQL-Orin user_ref cross verification PASS: 6 registered Fixture users match exactly.'
