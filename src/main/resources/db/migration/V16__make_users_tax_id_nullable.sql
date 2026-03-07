-- Allow user registration without CPF (tax_id).
-- Previously, V2 created `users.tax_id` as NOT NULL, which caused 500 errors on /auth/register
-- when the request payload did not include taxId.
ALTER TABLE users
    ALTER COLUMN tax_id DROP NOT NULL;

