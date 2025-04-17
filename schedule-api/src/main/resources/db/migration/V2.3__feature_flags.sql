CREATE TABLE public.feature_flags_features (
	feat_uid varchar(100) NOT NULL,
	"enable" int4 NOT NULL,
	description varchar(1000) NULL,
	strategy varchar(1000) NULL,
	"expression" varchar(255) NULL,
	groupname varchar(100) NULL,
	CONSTRAINT feature_flags_features_pkey PRIMARY KEY (feat_uid)
);

INSERT INTO public.feature_flags_features (feat_uid, "enable", description, strategy, "expression", groupname) VALUES ('vfLoginFeature', 0, 'Use Vereinsflieger.de for user authentication', NULL, NULL, NULL);