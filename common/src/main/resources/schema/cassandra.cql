CREATE TABLE {prefix}actions (
  time timestamp,
  actor_uuid uuid,
  actor_name text,
  type ascii,
  acted_uuid uuid,
  acted_name text,
  action text
  PRIMARY KEY(time)
);

CREATE TABLE {prefix}groups (
  name text,
  PRIMARY KEY(name)
);


CREATE TABLE users (
  uuid uuid,
  name text,
  permissions frozen<set<permission>>,
  primaryGroup text,
  PRIMARY KEY(uuid)
);

CREATE TYPE permission (
  id text,
  value boolean,
  server text,
  world text,
  expiry timestamp,
  contexts map<text,text>,
);