-- Seed role reference data
INSERT INTO auth_role (id, name) VALUES ('00000000-0000-0000-0000-000000000001', 'ROLE_USER');
INSERT INTO auth_role (id, name) VALUES ('00000000-0000-0000-0000-000000000002', 'ROLE_ADMIN');
INSERT INTO auth_role (id, name) VALUES ('00000000-0000-0000-0000-000000000003', 'ROLE_ALARM_ADMIN');
INSERT INTO auth_role (id, name) VALUES ('00000000-0000-0000-0000-000000000004', 'ROLE_SCHEDULE_ADMIN');

-- Migrate existing user roles to the join table
INSERT INTO auth_user_roles (user_id, role_id)
SELECT u.id, r.id
FROM auth_user u
JOIN auth_role r ON r.name = u.user_role;
