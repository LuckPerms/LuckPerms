CREATE TYPE permission (
  id text,
  value boolean,
  server text,
  world text,
  expiry timestamp,
  contexts map<text,text>,
);

CREATE TABLE {prefix}actions (
  time timestamp,
  actor_uuid uuid,
  actor_name text,
  type ascii,
  acted_uuid uuid,
  acted_name text,
  action text,
  PRIMARY KEY(time)
);

CREATE TABLE {prefix}groups (
  name text,
  permissions frozen<set<permission>>,
  PRIMARY KEY(name)
);

CREATE TABLE {prefix}users (
  uuid uuid,
  name text,
  permissions frozen<set<permission>>,
  primaryGroup text,
  PRIMARY KEY(uuid)
);

CREATE TABLE {prefix}tracks (
  name text,
  groups list<text>,
  PRIMARY KEY(name)
);

CREATE TABLE {prefix}uuid_to_name (
  uuid uuid,
  name text,
  PRIMARY KEY(uuid)
);

CREATE TABLE {prefix}name_to_uuid (
  name text,
  uuid uuid,
  PRIMARY KEY(name)
);