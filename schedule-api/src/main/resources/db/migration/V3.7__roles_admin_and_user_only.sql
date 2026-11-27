-- Users are either administrators (ROLE_ADMIN) or members (ROLE_USER). The module roles are replaced by the rights
-- that already unlocked the same functions, so nobody loses access.

INSERT INTO auth_user_permissions (user_id, permission)
SELECT DISTINCT ur.user_id,
       CASE r.name
           WHEN 'ROLE_SCHEDULE_ADMIN' THEN 'PERMISSION_SCHEDULE_MANAGE'
           WHEN 'ROLE_ALARM_ADMIN' THEN 'PERMISSION_ALARM_MANAGE'
           ELSE 'PERMISSION_MAINTENANCE_MANAGE'
       END
FROM auth_user_roles ur
JOIN auth_role r ON r.id = ur.role_id
WHERE r.name IN ('ROLE_SCHEDULE_ADMIN', 'ROLE_ALARM_ADMIN', 'ROLE_MAINTENANCE_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM auth_user_permissions p
      WHERE p.user_id = ur.user_id
        AND p.permission = CASE r.name
            WHEN 'ROLE_SCHEDULE_ADMIN' THEN 'PERMISSION_SCHEDULE_MANAGE'
            WHEN 'ROLE_ALARM_ADMIN' THEN 'PERMISSION_ALARM_MANAGE'
            ELSE 'PERMISSION_MAINTENANCE_MANAGE'
        END);

DELETE FROM auth_user_roles
WHERE role_id IN (SELECT id FROM auth_role WHERE name NOT IN ('ROLE_USER', 'ROLE_ADMIN'));

DELETE FROM auth_role WHERE name NOT IN ('ROLE_USER', 'ROLE_ADMIN');

-- A user who only had a module role is a member now.
INSERT INTO auth_user_roles (user_id, role_id)
SELECT u.id, r.id
FROM auth_user u
JOIN auth_role r ON r.name = 'ROLE_USER'
WHERE NOT EXISTS (SELECT 1 FROM auth_user_roles ur WHERE ur.user_id = u.id);

ALTER TABLE auth_role ADD CONSTRAINT auth_role_name_check CHECK (name IN ('ROLE_USER', 'ROLE_ADMIN'));

ALTER TABLE auth_user_permissions ADD CONSTRAINT auth_user_permissions_permission_check CHECK (permission IN (
    'PERMISSION_SCHEDULE_MANAGE', 'PERMISSION_ALARM_MANAGE', 'PERMISSION_MAINTENANCE_MANAGE', 'PERMISSION_OPERATIONS_MANAGE'));
