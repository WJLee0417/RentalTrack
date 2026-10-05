\set ON_ERROR_STOP on

BEGIN;

DO $migration$
BEGIN
    IF to_regclass('public.face_profile') IS NULL THEN
        RAISE EXCEPTION
            'public.face_profile must exist before removing registration_status';
    END IF;
END
$migration$;

ALTER TABLE public.face_profile
    DROP COLUMN IF EXISTS registration_status;

DO $migration$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM pg_attribute
         WHERE attrelid = 'public.face_profile'::REGCLASS
           AND attname = 'registration_status'
           AND attnum > 0
           AND NOT attisdropped
    ) THEN
        RAISE EXCEPTION
            'registration_status still exists after migration';
    END IF;
END
$migration$;

COMMIT;
