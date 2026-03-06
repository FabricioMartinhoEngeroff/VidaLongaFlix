-- Remove test users inserted by V2 seed (not needed in production)
DELETE FROM users_roles WHERE user_id = 'd193afd4-9222-4150-aadb-5167405a771c';
DELETE FROM users WHERE id = 'd193afd4-9222-4150-aadb-5167405a771c';
