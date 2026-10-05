\set ON_ERROR_STOP on

BEGIN;

CREATE EXTENSION IF NOT EXISTS vector;

DO $migration$
DECLARE
    actual_vector_version TEXT;
BEGIN
    SELECT extversion
      INTO actual_vector_version
      FROM pg_extension
     WHERE extname = 'vector';

    IF actual_vector_version IS DISTINCT FROM '0.8.5' THEN
        RAISE EXCEPTION
            'Expected vector extension version 0.8.5, found %',
            COALESCE(actual_vector_version, '<missing>');
    END IF;
END
$migration$;

CREATE TABLE IF NOT EXISTS public.face_profile (
    face_profile_id UUID NOT NULL,
    user_ref UUID NOT NULL,
    embedding VECTOR(512) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    template_version INTEGER NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_face_profile
        PRIMARY KEY (face_profile_id),
    CONSTRAINT uk_face_profile_user_ref
        UNIQUE (user_ref),
    CONSTRAINT ck_face_profile_template_version_positive
        CHECK (template_version > 0)
);

DO $migration$
DECLARE
    face_profile_oid OID;
    actual_columns TEXT;
BEGIN
    SELECT to_regclass('public.face_profile')::OID
      INTO face_profile_oid;

    IF face_profile_oid IS NULL THEN
        RAISE EXCEPTION 'public.face_profile was not created';
    END IF;

    SELECT string_agg(
               format(
                   '%I %s%s',
                   attribute.attname,
                   format_type(attribute.atttypid, attribute.atttypmod),
                   CASE WHEN attribute.attnotnull THEN ' NOT NULL' ELSE '' END
               ),
               ', ' ORDER BY attribute.attnum
           )
      INTO actual_columns
      FROM pg_attribute AS attribute
     WHERE attribute.attrelid = face_profile_oid
       AND attribute.attnum > 0
       AND NOT attribute.attisdropped;

    IF EXISTS (
        SELECT 1
          FROM (
              VALUES
                  ('face_profile_id', 'uuid', TRUE),
                  ('user_ref', 'uuid', TRUE),
                  ('embedding', 'vector(512)', TRUE),
                  ('model_version', 'character varying(50)', TRUE),
                  ('template_version', 'integer', TRUE),
                  ('registered_at', 'timestamp with time zone', TRUE),
                  ('updated_at', 'timestamp with time zone', TRUE)
          ) AS expected(column_name, formatted_type, is_not_null)
          LEFT JOIN pg_attribute AS attribute
            ON attribute.attrelid = face_profile_oid
           AND attribute.attname = expected.column_name
           AND attribute.attnum > 0
           AND NOT attribute.attisdropped
         WHERE attribute.attname IS NULL
            OR format_type(attribute.atttypid, attribute.atttypmod)
                   <> expected.formatted_type
            OR attribute.attnotnull <> expected.is_not_null
    ) THEN
        RAISE EXCEPTION
            'public.face_profile column contract mismatch. Actual columns: %',
            COALESCE(actual_columns, '<none>');
    END IF;

    IF (
        SELECT count(*)
          FROM pg_attribute AS attribute
         WHERE attribute.attrelid = face_profile_oid
           AND attribute.attnum > 0
           AND NOT attribute.attisdropped
    ) <> 7 THEN
        RAISE EXCEPTION
            'public.face_profile must contain exactly seven columns. Actual columns: %',
            COALESCE(actual_columns, '<none>');
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint AS constraint_record
         WHERE constraint_record.conrelid = face_profile_oid
           AND constraint_record.conname = 'pk_face_profile'
           AND constraint_record.contype = 'p'
           AND constraint_record.convalidated
           AND constraint_record.conkey = ARRAY[
               (
                   SELECT attnum
                     FROM pg_attribute
                    WHERE attrelid = face_profile_oid
                      AND attname = 'face_profile_id'
                      AND NOT attisdropped
               )
           ]::SMALLINT[]
    ) THEN
        RAISE EXCEPTION
            'Expected primary key pk_face_profile(face_profile_id) is missing or invalid';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint AS constraint_record
         WHERE constraint_record.conrelid = face_profile_oid
           AND constraint_record.conname = 'uk_face_profile_user_ref'
           AND constraint_record.contype = 'u'
           AND constraint_record.convalidated
           AND constraint_record.conkey = ARRAY[
               (
                   SELECT attnum
                     FROM pg_attribute
                    WHERE attrelid = face_profile_oid
                      AND attname = 'user_ref'
                      AND NOT attisdropped
               )
           ]::SMALLINT[]
    ) THEN
        RAISE EXCEPTION
            'Expected unique constraint uk_face_profile_user_ref(user_ref) is missing or invalid';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint AS constraint_record
         WHERE constraint_record.conrelid = face_profile_oid
           AND constraint_record.conname = 'ck_face_profile_template_version_positive'
           AND constraint_record.contype = 'c'
           AND constraint_record.convalidated
           AND regexp_replace(
                   pg_get_expr(
                       constraint_record.conbin,
                       constraint_record.conrelid
                   ),
                   '\s+',
                   '',
                   'g'
               ) = '(template_version>0)'
    ) THEN
        RAISE EXCEPTION
            'Expected check ck_face_profile_template_version_positive(template_version > 0) is missing or invalid';
    END IF;
END
$migration$;

COMMIT;
