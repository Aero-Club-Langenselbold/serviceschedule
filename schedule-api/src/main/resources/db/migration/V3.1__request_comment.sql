CREATE TABLE request_comment (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    request_id character varying(36) NOT NULL,
    author_id character varying(36),
    content text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT request_comment_pkey PRIMARY KEY (id),
    CONSTRAINT fk_request_comment_request FOREIGN KEY (request_id) REFERENCES request (id) ON DELETE CASCADE,
    CONSTRAINT fk_request_comment_author FOREIGN KEY (author_id) REFERENCES auth_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_request_comment_request ON request_comment (request_id);
