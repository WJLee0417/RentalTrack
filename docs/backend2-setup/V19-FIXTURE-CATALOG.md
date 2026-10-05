# V19 Demo Fixture Catalog

## 1. Status and scope

- Status: `LOCKED`
- Schema baseline: Flyway V1 through V19
- Baseline commit: `f7971d937ae5eb4b1b2b25ffd0434ec484d69b76`
- This catalog contains synthetic demonstration data only.
- No production account, real student number, real email address, or personal
  name may be substituted for a catalog value.
- Fixture SQL must live outside `db/migration` and must not modify the schema.
- Reapplication and reset behavior is governed by
  [V19-FIXTURE-RESET-STRATEGY.md](V19-FIXTURE-RESET-STRATEGY.md).

## 2. Shared account credential

All fixture user and administrator accounts use the same demonstration-only
credential. Fixture SQL stores only the BCrypt hash.

| Field | Fixed value |
| --- | --- |
| Demonstration password | `fixture-demo-only!` |
| BCrypt strength | `10` |
| BCrypt hash | `$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u` |

The password and hash must never be used for a production or personal account.

## 3. User scenario catalog

`rental_eligibility_evaluated_at` uses the fixed fixture timestamp
`2026-08-03 00:00:00.000000` when the projection has been evaluated. The
unevaluated scenario is the only row for which it is `NULL`.

| Scenario key | Student number (`user_id`) | `user_ref` | Login ID | Synthetic name | Face | Authoritative blocker | Projection |
| --- | --- | --- | --- | --- | ---: | --- | --- |
| `eligible` | `202600001` | `11000000-0000-4000-8000-000000000001` | `eligible.fixture@billisan.test` | `Eligible Fixture User` | true | none | `true` + evaluated timestamp |
| `face-unregistered` | `202600002` | `11000000-0000-4000-8000-000000000002` | `face-unregistered.fixture@billisan.test` | `Face Unregistered Fixture User` | false | `face_registered = false`; stage-40 partial outstanding Settlement | `false` + evaluated timestamp |
| `active-rental` | `202600003` | `11000000-0000-4000-8000-000000000003` | `active-rental.fixture@billisan.test` | `Active Rental Fixture User` | true | `rental.status = ACTIVE`; stage-40 unpaid overdue Settlement | `false` + evaluated timestamp |
| `return-recovery` | `202600004` | `11000000-0000-4000-8000-000000000004` | `return-recovery.fixture@billisan.test` | `Return Recovery Fixture User` | true | `return_attempt.status = RECOVERY_REQUIRED` | `false` + evaluated timestamp |
| `unpaid-settlement` | `202600005` | `11000000-0000-4000-8000-000000000005` | `unpaid-settlement.fixture@billisan.test` | `Unpaid Settlement Fixture User` | true | pending DAMAGE Settlement with outstanding amount | `false` + evaluated timestamp |
| `unevaluated` | `202600006` | `11000000-0000-4000-8000-000000000006` | `unevaluated.fixture@billisan.test` | `Unevaluated Fixture User` | true | none; projection has not run | `false` + `NULL` |

The `return-recovery` scenario also retains its rental in `RETURNING`. This is
intentional: the V14 return-attempt trigger requires a non-failed return to
belong to a `RETURNING` rental. The scenario must not manufacture an invalid
state merely to isolate one predicate.

The application login response field named `userRef` carries the opaque
`user_ref` UUID, not the nine-digit `user_id` student-number primary key. The
current login response exposes `faceRegistered` but does not expose
`rentalEligible`; stage 20 therefore verifies the eligibility projection in
SQL. Rental and return REST behavior remains outside this fixture stage.

## 4. Authority record IDs

### Shared station and slots

| Record | Fixed UUID | Purpose |
| --- | --- | --- |
| Station | `22000000-0000-4000-8000-000000000001` | `Fixture Station 01`; initial state `AVAILABLE/ONLINE` |
| Slot 1 | `33000000-0000-4000-8000-000000000001` | Active-rental checkout slot |
| Slot 2 | `33000000-0000-4000-8000-000000000002` | Return-recovery checkout slot |
| Slot 3 | `33000000-0000-4000-8000-000000000003` | Return-recovery destination slot |
| Slot 4 | `33000000-0000-4000-8000-000000000004` | Unpaid-settlement checkout slot |
| Slot 5 | `33000000-0000-4000-8000-000000000005` | Completed damaged-return slot |

The station boot identifier is `fixture-boot-station-01`.
Before scenario scripts run, all five slots use the neutral reference state
`AVAILABLE/EMPTY/LOCKED/EMPTY` in
`service_status/occupancy_status/lock_status/item_condition` order. Scenario
scripts 30 and 40 own and verify the final mutable slot states.

### Scenario-owned business records

| Scenario | Table | Fixed UUID | Request key | Required state |
| --- | --- | --- | --- | --- |
| `active-rental` | `rental` | `44000000-0000-4000-8000-000000000001` | `fixture-rental-active` | `ACTIVE` |
| `return-recovery` | `rental` | `44000000-0000-4000-8000-000000000002` | `fixture-rental-return-recovery` | `RETURNING` |
| `return-recovery` | `return_attempt` | `55000000-0000-4000-8000-000000000001` | `fixture-return-recovery-required` | `RECOVERY_REQUIRED` |
| `unpaid-settlement` | `rental` | `44000000-0000-4000-8000-000000000003` | `fixture-rental-unpaid-settlement` | inserted as `RETURNING`, completed by the return trigger |
| `unpaid-settlement` | `return_attempt` | `55000000-0000-4000-8000-000000000002` | `fixture-return-unpaid-completed` | `COMPLETED` |
| `unpaid-settlement` | `damage_inspection` | `66000000-0000-4000-8000-000000000001` | `fixture-inspection-unpaid-damage` | completed, `DAMAGED`, administrator-reviewed |
| `unpaid-settlement` | `settlement` | `77000000-0000-4000-8000-000000000001` | N/A | `PENDING`, `amount = 7000`, `paid_amount = 0` |
| `completed-normal` | `rental` | `44000000-0000-4000-8000-000000000004` | `fixture-rental-normal-completed` | inserted as `RETURNING`, completed by the return trigger |
| `completed-normal` | `return_attempt` | `55000000-0000-4000-8000-000000000003` | `fixture-return-normal-completed` | `COMPLETED` |
| `completed-normal` | `damage_inspection` | `66000000-0000-4000-8000-000000000002` | `fixture-inspection-normal-decided` | completed, `NORMAL`, administrator-reviewed |
| `admin-review-pending` | `damage_inspection` | `66000000-0000-4000-8000-000000000003` | `fixture-inspection-review-pending` | completed AI result, administrator decision pending |
| `ai-failed` | `rental` | `44000000-0000-4000-8000-000000000006` | `fixture-rental-ai-failed` | inserted as `RETURNING`, completed by the return trigger |
| `ai-failed` | `return_attempt` | `55000000-0000-4000-8000-000000000004` | `fixture-return-ai-failed-completed` | `COMPLETED` |
| `ai-failed` | `damage_inspection` | `66000000-0000-4000-8000-000000000004` | `fixture-inspection-ai-failed` | `FAILED`, AI output columns `NULL` |
| `failed-rental` | `rental` | `44000000-0000-4000-8000-000000000005` | `fixture-rental-face-ineligible` | `FAILED`, `USER_NOT_ELIGIBLE` |
| `recovery-failed-attempt` | `return_attempt` | `55000000-0000-4000-8000-000000000005` | `fixture-return-recovery-failed` | `FAILED`; releases the rental claim before retry |
| `payment-partial-parent` | `rental` | `44000000-0000-4000-8000-000000000007` | `fixture-rental-payment-partial` | inserted as `RETURNING`, completed by the return trigger |
| `payment-partial-parent` | `return_attempt` | `55000000-0000-4000-8000-000000000006` | `fixture-return-payment-partial` | `COMPLETED` |

Stage 30 also owns the following independent operational records. A
`DEVICE_OPERATION` never carries `rental_id` or `return_attempt_id`, and its
evidence JSON contains no student-number identity key at any depth.

| Scenario | Table | Fixed UUID | Request/command key | Required state |
| --- | --- | --- | --- | --- |
| `device-command` | `device_operation` | `99000000-0000-4000-8000-000000000001` | `fixture-device-command-active-lock` | command-only `REQUESTED` |
| `device-terminal` | `device_operation` | `99000000-0000-4000-8000-000000000002` | `fixture-device-command-recovery-lock` | terminal `FAILED` / `OPERATION_FAILED` |
| `face-register-succeeded` | `face_profile_sync_operation` | `aa000000-0000-4000-8000-000000000001` | `fixture-face-register-eligible` | `REGISTER` / `SUCCEEDED`, keyed by `user_ref` |

After stage 30, the mutable Slot state is fixed as follows:

| Slot | Service | Occupancy | Lock | Item condition |
| --- | --- | --- | --- | --- |
| 1 | `AVAILABLE` | `EMPTY` | `LOCKED` | `EMPTY` |
| 2 | `AVAILABLE` | `EMPTY` | `LOCKED` | `EMPTY` |
| 3 | `ADMIN_REVIEW` | `OCCUPIED` | `ERROR` | `UNKNOWN` |
| 4 | `AVAILABLE` | `EMPTY` | `LOCKED` | `EMPTY` |
| 5 | `OUT_OF_SERVICE` | `OCCUPIED` | `LOCKED` | `DAMAGED` |

The 7,000 KRW DAMAGE amount is required by
`CK_SETTLEMENT_REASON_AMOUNT`; stage 40 creates that immutable financial row.

### Settlement and payment records

Every stage-40 Settlement is initially inserted as `PENDING` with
`paid_amount = 0` and `paid_at = NULL`. The final values below are materialized
only by successful-payment triggers.

| Scenario | Settlement UUID | Rental UUID | Reason/amount | Final payment state |
| --- | --- | --- | --- | --- |
| `damage-unpaid` | `77000000-0000-4000-8000-000000000001` | `44000000-0000-4000-8000-000000000003` | `DAMAGE` / 7,000 | `PENDING`, paid 0; directly references inspection `66000000-0000-4000-8000-000000000001` |
| `overdue-unpaid` | `77000000-0000-4000-8000-000000000002` | `44000000-0000-4000-8000-000000000001` | `OVERDUE` / 1,000 | `PENDING`, paid 0 |
| `overdue-partially-paid` | `77000000-0000-4000-8000-000000000003` | `44000000-0000-4000-8000-000000000007` | `OVERDUE` / 2,000 | `PENDING`, paid 1,000 |
| `adjustment-paid` | `77000000-0000-4000-8000-000000000004` | `44000000-0000-4000-8000-000000000004` | `ADJUSTMENT` / 4,000 | `PAID`, paid 4,000 |

| Scenario | Payment UUID | Creation request key | Provider mapping | Amount |
| --- | --- | --- | --- | ---: |
| `partial-payment-success` | `78000000-0000-4000-8000-000000000001` | `fixture-payment-partial-success` | `TOSS_SANDBOX`, `DONE`, `SUCCEEDED` | 1,000 |
| `completed-payment-success` | `78000000-0000-4000-8000-000000000002` | `fixture-payment-completed-success` | `TOSS_SANDBOX`, `DONE`, `SUCCEEDED` | 4,000 |
| `failed-payment` | `78000000-0000-4000-8000-000000000003` | `fixture-payment-failed` | `TOSS_SANDBOX`, `ABORTED`, `FAILED` | 1,000 |
| `payment-reconciliation-required` | `78000000-0000-4000-8000-000000000004` | `fixture-payment-reconciliation` | `TOSS_SANDBOX`, `PARTIAL_CANCELED`, `RECONCILIATION_REQUIRED` | 7,000 |

The failed and reconciliation-required attempts add no Settlement credit. The
two `SUCCEEDED` rows and the DAMAGE Settlement are immutable ledger records;
normal fixture reapplication validates them without UPDATE or DELETE.

### Fail-fast verification

Run `90-verify-fixtures.sql` last and without the MySQL `--force` option. It
catalogs 41 expected rows across the 11 business tables and validates them in a
consistent read-only snapshot. Every assertion writes only to a temporary Guard
table; a mismatch violates the CHECK named for that assertion group and stops
the client before any `PASS` result is printed.

The eligibility comparison repeats the application authority query exactly:
face registration, no `REQUESTED`/`ACTIVE`/`RETURNING` Rental, no
`RECOVERY_REQUIRED` Return Attempt, and no outstanding `PENDING` Settlement.
Evaluated projections must equal that result. The unevaluated scenario remains
fail-closed as `false + NULL` even though its current authority result is true.

The verification file is read-only with respect to business and Flyway tables.
It does not repair drift, consume Trigger Guards, or normalize fixture values.

The administrator reviewing the synthetic damage inspection is fixed as:

| Field | Fixed value |
| --- | --- |
| `admin_id` | `88000000-0000-4000-8000-000000000001` |
| `login_id` | `admin.fixture@billisan.test` |
| `name` | `Fixture Administrator` |
| `password_hash` | shared demonstration-only BCrypt hash |

## 5. Catalog invariants

1. Student numbers are exactly nine decimal digits.
2. Entity IDs are lowercase canonical UUID strings and are never generated at
   fixture load time.
3. Every request, command, event, or boot key begins with `fixture-`.
4. Login IDs are lowercase synthetic addresses under the reserved `.test` TLD.
5. Each blocker scenario owns its rental/return/inspection/settlement records;
   only the station is shared.
6. The eligible and unevaluated users have the same authoritative eligibility
   facts. Their difference is exclusively whether the projection was evaluated.
7. The internal `settlement_payment_mutation_guard` table receives no fixture
   rows.
