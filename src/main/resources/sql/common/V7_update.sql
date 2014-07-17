CREATE TABLE ${UuidDisplayNameCache} (
  name VARCHAR(255),
  display_name VARCHAR(255) NOT NULL,
  uuid VARCHAR(255) NOT NULL,
  timestamp TIMESTAMP NOT NULL,
  PRIMARY KEY (name)
);
CREATE SEQUENCE ${UuidDisplayNameCache}_seq;
