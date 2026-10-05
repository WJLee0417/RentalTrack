# V19 Demo Fixture Reset Strategy

## 1. Decision

- Status: `LOCKED`
- Schema baseline: Flyway V1 through V19
- Normal reapplication: conditionally insert a fixed ID, then verify the full
  expected row.
- Complete reset: rebuild a disposable Demo database and reapply V1 through
  V19.
- Shared EC2 database: never run `99-reset-fixtures.sql` without explicit
  operator authorization.

Fixture loading and fixture removal are operational data procedures. They are
not Flyway migrations and must remain outside `db/migration`.

## 2. Why reverse deletion is not a reset

V16 through V18 deliberately make parts of the financial and review history
irreversible.

| Migration | Trigger protection | Database error |
| --- | --- | --- |
| V16 | A `SUCCEEDED` payment attempt cannot be deleted | `SUCCEEDED_PAYMENT_DELETE_FORBIDDEN` |
| V16 | Settlement payment materialization cannot be rewritten directly | `SETTLEMENT_PAYMENT_FIELDS_MANAGED` |
| V17 | Review decision, reviewer, and review time are frozen after settlement | `SETTLED_DAMAGE_REVIEW_AUDIT_IMMUTABLE` |
| V18 | A damage settlement cannot replace or clear its inspection basis | `DAMAGE_SETTLEMENT_BASIS_IMMUTABLE` |
| V18 | A settlement with a damage basis cannot be deleted | `DAMAGE_SETTLEMENT_DELETE_FORBIDDEN` |

Consequently, neither a generic reverse-order `DELETE` nor a fixture-specific
best-effort delete can promise a complete reset.

## 3. Normal fixture reapplication contract

The loader must be safe to run repeatedly without changing an existing row.
For each catalog record it performs these logical steps:

1. Insert the complete fixed row only when its primary key does not exist.
2. Select the row by primary key.
3. Compare every fixture-owned column with the catalog value, including
   foreign keys, request keys, authoritative status, projection, and fixed
   timestamps.
4. Raise `FIXTURE_CONFLICT` and roll back when any value differs.

Column ownership follows the numbered fixture stages. Reference-data scripts
verify stable identity and relationship columns; a later scenario script that
sets a final mutable state owns and verifies that state. This permits the full
numbered fixture sequence to be reapplied without an earlier stage reverting a
later stage.

The implementation must use an explicit conditional `INSERT ... SELECT ...
WHERE NOT EXISTS (...)`. The following shortcuts are forbidden:

- `INSERT IGNORE`
- `REPLACE`
- `INSERT ... ON DUPLICATE KEY UPDATE`
- an `UPDATE` that attempts to restore a catalog row

A collision on another unique key while the fixed primary key is absent must
remain a hard SQL error. It must not be hidden or converted into an update.

The loader must run as one transaction. A validation failure leaves all rows
at their pre-run values. Reapplication never calls the reset script.

## 4. `99-reset-fixtures.sql` contract

The reset script is a manual recovery aid, not the normal fixture workflow. It
must never be called by Spring startup, Flyway, CI deployment, or another
fixture script.

### 4.1 Target guard

Before reading or deleting fixture rows, the script must verify both:

1. `DATABASE()` is the explicitly designated disposable Demo database.
2. The caller set a separate session confirmation value required by the
   script.

Failure raises `RESET_TARGET_REJECTED` before any DML. A shared EC2 database is
not an allowed target merely because it contains fixture IDs.

### 4.2 Immutable-ledger preflight

All preflight checks run before the first deletion. The script raises
`REBUILD_REQUIRED` when any fixture-owned graph contains one of these facts:

- a `payment_attempt` in `SUCCEEDED` state;
- a `settlement` with a non-null `damage_inspection_id`;
- a damage inspection currently referenced by a settlement;
- another state protected from removal by the V16 through V18 ledger
  contract.

The catalog's administrator-reviewed damage settlement intentionally satisfies
this preflight. After that row exists, a complete reset requires rebuilding the
disposable Demo database.

### 4.3 Deletable subset

Only after every preflight check passes may the script begin one transaction
and delete rows selected by the exact fixed IDs in the catalog. It deletes
children before parents and never broadens selection by email domain, name,
date, or `fixture-` prefix alone.

The removable order is:

1. non-succeeded fixture payment attempts;
2. deletable settlements;
3. unreferenced damage inspections;
4. return attempts;
5. rentals;
6. optional device and face-sync fixture records;
7. slots;
8. station;
9. fixture administrator and users.

The script must not delete from `settlement_payment_mutation_guard`. It commits
only after verifying that every deletable catalog ID is absent. Any SQL error
rolls back the transaction.

### 4.4 Prohibited reset mechanisms

The script and its runbook must not use:

- `TRUNCATE TABLE`;
- `SET FOREIGN_KEY_CHECKS = 0`;
- Trigger drop, replacement, disablement, or bypass;
- direct mutation of protected ledger or review fields;
- wildcard deletion of non-catalog rows.

## 5. Complete rebuild procedure

When reset reports `REBUILD_REQUIRED`, an authorized operator uses a database
that is explicitly disposable:

1. Confirm that the target is the disposable Demo database, not the shared or
   production database.
2. Drop and recreate only that Demo database using its owning account.
3. Apply Flyway V1 through V19 to the empty database.
4. Run Flyway validate.
5. Start Spring with `hibernate.ddl-auto=validate` and confirm validation.
6. Apply the fixture loader and run its scenario verification.

The database rebuild procedure is intentionally separate from
`99-reset-fixtures.sql`; the reset script reports the required action but does
not drop or recreate a database.

## 6. Result signals

| Signal | Meaning | Required action |
| --- | --- | --- |
| `FIXTURE_CONFLICT` | A fixed ID exists with different content | Investigate drift; do not overwrite |
| `REBUILD_REQUIRED` | Immutable ledger/review history prevents full removal | Rebuild the disposable Demo database |
| `RESET_TARGET_REJECTED` | Database identity or session confirmation is unsafe | Stop and select the correct target |

Each signal must make the MySQL client exit non-zero so automation cannot
mistake a partial operation for success.
