\set ON_ERROR_STOP on

BEGIN;

CREATE TEMPORARY TABLE expected_face_profile (
    face_profile_id UUID PRIMARY KEY,
    user_ref UUID NOT NULL UNIQUE
) ON COMMIT DROP;

INSERT INTO expected_face_profile (face_profile_id, user_ref)
VALUES
    (
        '7ba948f4-4e12-4dae-abf1-aa80ec5df05e',
        'f6f509d4-c001-446c-b6b7-537d65a093c3'
    ),
    (
        '12660140-9643-4bbe-a217-d35417828b75',
        'f86a8791-c02b-4e50-b761-d9dd009909a9'
    ),
    (
        'e5586e12-f536-4673-8ae3-c45fad0007fa',
        '0b8255a1-70e4-45af-80f2-b38e4164ea06'
    ),
    (
        '672dde6d-37fe-46a8-ac39-341dd2360214',
        '59b97d2b-a37d-4f75-bd33-4beb1bd3ac76'
    ),
    (
        '7ab49ad1-5e45-4d82-bfbc-b389e623b19b',
        'd8a6f8d6-3c43-4789-b980-a9f6918d53b8'
    ),
    (
        '5b7a58c1-67e7-4ba1-b8e1-1231fc45cf73',
        '3650e75c-ab2b-4455-8f00-c691fd2e66fb'
    );

DO $verification$
DECLARE
    failure_detail TEXT;
BEGIN
    IF to_regclass('public.face_profile') IS NULL THEN
        RAISE EXCEPTION 'FACE_PROFILE_VERIFY_FAILED: public.face_profile is missing';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM pg_attribute
         WHERE attrelid = 'public.face_profile'::REGCLASS
           AND attname = 'registration_status'
           AND attnum > 0
           AND NOT attisdropped
    ) THEN
        RAISE EXCEPTION
            'FACE_PROFILE_VERIFY_FAILED: legacy registration_status column is forbidden; row existence is the registration authority';
    END IF;

    IF (SELECT count(*) FROM public.face_profile) <> 6 THEN
        RAISE EXCEPTION
            'FACE_PROFILE_VERIFY_FAILED: expected 6 profiles, found %',
            (SELECT count(*) FROM public.face_profile);
    END IF;

    SELECT string_agg(expected.face_profile_id::TEXT, ', ' ORDER BY expected.face_profile_id)
      INTO failure_detail
      FROM expected_face_profile AS expected
      LEFT JOIN public.face_profile AS actual
        ON actual.face_profile_id = expected.face_profile_id
     WHERE actual.face_profile_id IS NULL;

    IF failure_detail IS NOT NULL THEN
        RAISE EXCEPTION
            'FACE_PROFILE_VERIFY_FAILED: missing face_profile_id: %',
            failure_detail;
    END IF;

    SELECT string_agg(expected.user_ref::TEXT, ', ' ORDER BY expected.user_ref)
      INTO failure_detail
      FROM expected_face_profile AS expected
      LEFT JOIN public.face_profile AS actual
        ON actual.user_ref = expected.user_ref
     WHERE actual.user_ref IS NULL;

    IF failure_detail IS NOT NULL THEN
        RAISE EXCEPTION
            'FACE_PROFILE_VERIFY_FAILED: missing user_ref: %',
            failure_detail;
    END IF;

    SELECT string_agg(actual.user_ref::TEXT, ', ' ORDER BY actual.user_ref)
      INTO failure_detail
      FROM public.face_profile AS actual
      LEFT JOIN expected_face_profile AS expected
        ON expected.user_ref = actual.user_ref
     WHERE expected.user_ref IS NULL;

    IF failure_detail IS NOT NULL THEN
        RAISE EXCEPTION
            'FACE_PROFILE_VERIFY_FAILED: unexpected user_ref: %',
            failure_detail;
    END IF;

    SELECT string_agg(
               actual.face_profile_id::TEXT || ' -> ' || actual.user_ref::TEXT,
               ', ' ORDER BY actual.face_profile_id
           )
      INTO failure_detail
      FROM public.face_profile AS actual
      JOIN expected_face_profile AS expected
        ON expected.face_profile_id = actual.face_profile_id
     WHERE actual.user_ref <> expected.user_ref;

    IF failure_detail IS NOT NULL THEN
        RAISE EXCEPTION
            'FACE_PROFILE_VERIFY_FAILED: face_profile_id/user_ref mapping mismatch: %',
            failure_detail;
    END IF;

    SELECT string_agg(user_ref::TEXT, ', ' ORDER BY user_ref)
      INTO failure_detail
      FROM (
          SELECT user_ref
            FROM public.face_profile
           GROUP BY user_ref
          HAVING count(*) > 1
      ) AS duplicate_user_ref;

    IF failure_detail IS NOT NULL THEN
        RAISE EXCEPTION
            'FACE_PROFILE_VERIFY_FAILED: duplicate user_ref: %',
            failure_detail;
    END IF;

    SELECT string_agg(face_profile_id::TEXT, ', ' ORDER BY face_profile_id)
      INTO failure_detail
      FROM (
          SELECT face_profile_id
            FROM public.face_profile
           GROUP BY face_profile_id
          HAVING count(*) > 1
      ) AS duplicate_face_profile_id;

    IF failure_detail IS NOT NULL THEN
        RAISE EXCEPTION
            'FACE_PROFILE_VERIFY_FAILED: duplicate face_profile_id: %',
            failure_detail;
    END IF;

    SELECT string_agg(user_ref::TEXT, ', ' ORDER BY user_ref)
      INTO failure_detail
      FROM public.face_profile
     WHERE model_version IS DISTINCT FROM 'insightface-buffalo_l';

    IF failure_detail IS NOT NULL THEN
        RAISE EXCEPTION
            'FACE_PROFILE_VERIFY_FAILED: unexpected model_version: %',
            failure_detail;
    END IF;

    SELECT string_agg(user_ref::TEXT, ', ' ORDER BY user_ref)
      INTO failure_detail
      FROM public.face_profile
     WHERE template_version IS DISTINCT FROM 1;

    IF failure_detail IS NOT NULL THEN
        RAISE EXCEPTION
            'FACE_PROFILE_VERIFY_FAILED: unexpected template_version: %',
            failure_detail;
    END IF;

    SELECT string_agg(user_ref::TEXT, ', ' ORDER BY user_ref)
      INTO failure_detail
      FROM public.face_profile
     WHERE vector_dims(embedding) IS DISTINCT FROM 512;

    IF failure_detail IS NOT NULL THEN
        RAISE EXCEPTION
            'FACE_PROFILE_VERIFY_FAILED: embedding dimension is not 512: %',
            failure_detail;
    END IF;

    SELECT string_agg(user_ref::TEXT, ', ' ORDER BY user_ref)
      INTO failure_detail
      FROM public.face_profile
     WHERE registered_at IS NULL
        OR updated_at IS NULL;

    IF failure_detail IS NOT NULL THEN
        RAISE EXCEPTION
            'FACE_PROFILE_VERIFY_FAILED: profile timestamp is NULL: %',
            failure_detail;
    END IF;

    SELECT string_agg(user_ref::TEXT, ', ' ORDER BY user_ref)
      INTO failure_detail
      FROM public.face_profile
     WHERE updated_at < registered_at;

    IF failure_detail IS NOT NULL THEN
        RAISE EXCEPTION
            'FACE_PROFILE_VERIFY_FAILED: updated_at precedes registered_at: %',
            failure_detail;
    END IF;
END
$verification$;

SELECT
    face_profile_id,
    user_ref,
    model_version,
    template_version,
    vector_dims(embedding) AS embedding_dimensions,
    registered_at,
    updated_at
FROM public.face_profile
ORDER BY user_ref;

COMMIT;
