CREATE TABLE auth_user (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    username character varying(50),
    user_role character varying(20),
    "password" character varying(128)
);

ALTER TABLE auth_user
    ADD CONSTRAINT auth_user_pkey PRIMARY KEY (id);

CREATE TABLE auth_user_aud (
	id varchar(36) NOT NULL,
	rev int4 NOT NULL,
	revtype int2,
    username character varying(50),
    user_role character varying(20),
    "password" character varying(128),
	CONSTRAINT auth_user_aud_pkey PRIMARY KEY (id, rev)
);

ALTER TABLE auth_user_aud ADD CONSTRAINT fk_auth_user_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev);