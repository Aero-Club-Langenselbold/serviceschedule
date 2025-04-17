CREATE TABLE public.feature_flags_features (
	feat_uid varchar(100) NOT NULL,
	"enable" int4 NOT NULL,
	description varchar(1000) NULL,
	strategy varchar(1000) NULL,
	"expression" varchar(255) NULL,
	groupname varchar(100) NULL,
	CONSTRAINT feature_flags_features_pkey PRIMARY KEY (feat_uid)
);

CREATE TABLE public.feature_flags_roles (
	feat_uid varchar(100) NOT NULL,
	role_name varchar(100) NOT NULL,
	CONSTRAINT feature_flags_roles_pkey PRIMARY KEY (feat_uid,role_name)
);

CREATE TABLE public.feature_flags_custom_properties (
	property_id varchar(100) NOT NULL,
	clazz varchar(255) NOT NULL,
	currentvalue varchar(255) NULL,
	fixedvalues varchar(1000) NULL,
	description varchar(1000) NULL,
	feat_uid varchar(100) NOT NULL,
	CONSTRAINT feature_flags_custom_properties_pkey PRIMARY KEY (property_id,feat_uid)
);
CREATE INDEX idx__feature_flags_custom_properties__feat_uid ON public.feature_flags_custom_properties (feat_uid);

INSERT INTO public.feature_flags_features (feat_uid, "enable", description, strategy, "expression", groupname) VALUES ('vfLoginFeature', 0, 'Use Vereinsflieger.de for user authentication', NULL, NULL, NULL);