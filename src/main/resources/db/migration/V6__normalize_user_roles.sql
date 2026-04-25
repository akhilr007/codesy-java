UPDATE app_users
SET role = UPPER(role)
WHERE role IN ('user', 'admin');