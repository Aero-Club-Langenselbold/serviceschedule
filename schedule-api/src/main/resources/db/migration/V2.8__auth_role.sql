CREATE TABLE auth_role (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    name character varying(20) NOT NULL,
    CONSTRAINT auth_role_pkey PRIMARY KEY (id),
    CONSTRAINT auth_role_name_unique UNIQUE (name)
);

CREATE TABLE auth_user_roles (
    user_id character varying(36) NOT NULL,
    role_id character varying(36) NOT NULL,
    CONSTRAINT auth_user_roles_pkey PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_auth_user_roles_user FOREIGN KEY (user_id) REFERENCES auth_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_auth_user_roles_role FOREIGN KEY (role_id) REFERENCES auth_role (id)
);
