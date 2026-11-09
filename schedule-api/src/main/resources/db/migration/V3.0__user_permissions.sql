CREATE TABLE auth_user_permissions (
    user_id character varying(36) NOT NULL,
    permission character varying(50) NOT NULL,
    CONSTRAINT auth_user_permissions_pkey PRIMARY KEY (user_id, permission),
    CONSTRAINT fk_auth_user_permissions_user FOREIGN KEY (user_id) REFERENCES auth_user (id) ON DELETE CASCADE
);
