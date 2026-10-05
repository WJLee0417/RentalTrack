# Demo Fixture User and Face Profile Catalog

## 1. Status and scope

- Status: `LOCKED_FOR_IMPLEMENTATION`
- Orin authority snapshot: the six rows recorded in section 2, captured at the
  per-row `updated_at` timestamps listed below.
- Purpose: fix the identity mapping used by the Backend MySQL demo Fixture and
  the Orin PostgreSQL face profiles.
- This catalog supersedes the user identity values in the existing V19 Fixture
  catalog when the Fixture SQL is updated.
- The six face-registered users start from the normal, rental-eligible baseline.
- The seventh user is the only face-unregistered baseline account.
- Catalog consumers must match records by the explicitly recorded UUID values,
  never by table row order.

## 2. Authoritative Orin face-profile snapshot

The current Orin PostgreSQL rows supplied for the demonstration are the latest
authority for Backend Fixture identity mapping. The catalog deliberately omits
the biometric `embedding` value.

| `user_id` | `USER_ACCOUNT.name` | Orin internal `face_profile_id` | Logical `user_ref` | `model_version` | `template_version` | `registered_at` | `updated_at` |
| --- | --- | --- | --- | --- | ---: | --- | --- |
| `202600001` | 제갈한동 | `7ba948f4-4e12-4dae-abf1-aa80ec5df05e` | `f6f509d4-c001-446c-b6b7-537d65a093c3` | `insightface-buffalo_l` | `1` | `2026-07-27 14:33:43.294763+09` | `2026-07-27 14:33:43.294763+09` |
| `202600002` | 장혜진 | `12660140-9643-4bbe-a217-d35417828b75` | `f86a8791-c02b-4e50-b761-d9dd009909a9` | `insightface-buffalo_l` | `1` | `2026-07-27 14:33:43.592076+09` | `2026-07-27 14:33:43.592076+09` |
| `202600003` | 곽도연 | `e5586e12-f536-4673-8ae3-c45fad0007fa` | `0b8255a1-70e4-45af-80f2-b38e4164ea06` | `insightface-buffalo_l` | `1` | `2026-07-27 14:33:44.174368+09` | `2026-07-27 14:33:44.174368+09` |
| `202600004` | 이다인 | `672dde6d-37fe-46a8-ac39-341dd2360214` | `59b97d2b-a37d-4f75-bd33-4beb1bd3ac76` | `insightface-buffalo_l` | `1` | `2026-07-27 14:33:44.613229+09` | `2026-07-27 14:33:44.613229+09` |
| `202600005` | 이원준 | `7ab49ad1-5e45-4d82-bfbc-b389e623b19b` | `d8a6f8d6-3c43-4789-b980-a9f6918d53b8` | `insightface-buffalo_l` | `1` | `2026-07-27 14:33:45.140921+09` | `2026-07-27 14:33:45.140921+09` |
| `202600006` | 이경호 | `5b7a58c1-67e7-4ba1-b8e1-1231fc45cf73` | `3650e75c-ab2b-4455-8f00-c691fd2e66fb` | `insightface-buffalo_l` | `1` | `2026-07-27 14:33:45.503783+09` | `2026-07-27 14:33:45.503783+09` |
| `202600007` | 얼굴 미등록 사용자 | 없음 | `bb14cac2-74d1-4b7f-a69f-aa88fcf8f55e` | 없음 | 없음 | 없음 | 없음 |

### Identifier rules

- `user_id` is the nine-digit demonstration student-number key in MySQL.
- `user_ref` is the cross-system logical identifier shared by Backend and Orin.
- Orin keeps `face_profile_id` only as an internal technical primary key.
- Orin must enforce `user_ref UUID NOT NULL UNIQUE` and process external
  profile operations by `user_ref`.
- `face_profile_id` must not be copied into MySQL or exposed through Backend or
  Frontend contracts.
- `registration_status` is prohibited and must not be reintroduced into the
  Orin schema, queries, dumps, or API contracts.
- The existence of a `face_profile` row for a `user_ref` is the sole Orin
  authority for face registration. Removing registration means deleting that
  row, not changing a status field.
- MySQL `USER_ACCOUNT.face_registered` remains a projection and must be
  synchronized from Orin row existence.
- All six registered rows use `model_version=insightface-buffalo_l` and
  `template_version=1`.
- The supplied `registered_at` and `updated_at` values are preserved as Orin
  reference data and are not copied into MySQL Fixture records.
- UUIDs must be copied from this table verbatim. They must not be generated from
  a student number, login ID, name, or row position.

## 3. Fixed login catalog

| `user_id` | Fixed `login_id` |
| --- | --- |
| `202600001` | `demo01.fixture@billisan.test` |
| `202600002` | `demo02.fixture@billisan.test` |
| `202600003` | `demo03.fixture@billisan.test` |
| `202600004` | `demo04.fixture@billisan.test` |
| `202600005` | `demo05.fixture@billisan.test` |
| `202600006` | `demo06.fixture@billisan.test` |
| `202600007` | `face-unregistered.fixture@billisan.test` |

- Login IDs are synthetic fixed values and are not derived from personal names.
- All login IDs must remain lowercase.
- No production account or actual personal email address may be substituted.

## 4. Shared demonstration credential

All seven users reuse the existing demonstration-only credential. Fixture SQL
stores only the BCrypt hash.

| Field | Fixed value |
| --- | --- |
| Demonstration password | `fixture-demo-only!` |
| BCrypt strength | `10` |
| BCrypt hash | `$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u` |

- The credential and hash must never be used for a production or personal
  account.
- The plaintext password must not be written to Fixture SQL.

## 5. Baseline projection

The evaluated timestamp is fixed at `2026-08-03 00:00:00.000000` for all seven
baseline accounts.

| Users | `face_registered` | `rental_eligible` | `rental_eligibility_evaluated_at` |
| --- | ---: | ---: | --- |
| `202600001` through `202600006` | `true` | `true` | fixed evaluated timestamp |
| `202600007` | `false` | `false` | fixed evaluated timestamp |

- The six registered users have no active rental, recovery-required return, or
  unpaid settlement in the baseline catalog.
- Scenario Fixtures may change a registered user's authoritative state after
  the baseline is established and must then recompute the projection.
- The face-unregistered user remains ineligible because
  `face_registered=false`.
- `rental_eligible=true` with a `NULL` evaluated timestamp is prohibited.

## 6. Personal and biometric data boundary

- Use of the six real demonstration names is allowed only in
  `USER_ACCOUNT.name`.
- Actual student numbers, personal email addresses, and personal passwords are
  prohibited.
- Face embeddings remain only in Orin PostgreSQL.
- Embeddings and original biometric dumps must not be copied into Backend
  Fixtures, logs, documents, or the Git repository.
- Backend Fixtures store the fixed `user_ref` values and the face-registration
  projection only.

## 7. Follow-up implementation checks

- Replace the previous Fixture user IDs, login IDs, names, and `user_ref` values
  with this catalog's fixed values.
- Add `202600007` without assigning an Orin `face_profile_id`.
- Verify all seven `user_id` values are unique nine-digit strings.
- Verify all seven `user_ref` values are unique canonical UUID strings.
- Verify the six Orin `face_profile_id` values are unique canonical UUIDs.
- Verify no Fixture derives a UUID or login ID from a name or student number.
- Verify the six registered users and their Orin rows are joined by the exact
  recorded `user_ref`, not by result ordering.
- Verify the seventh user's `user_ref` has no Orin face-profile row.
