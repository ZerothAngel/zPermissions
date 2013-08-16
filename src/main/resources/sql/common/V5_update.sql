CREATE TABLE ${DataVersion} (
  name VARCHAR(255),
  version BIGINT NOT NULL,
  timestamp TIMESTAMP NOT NULL,
  PRIMARY KEY (name)
);
CREATE SEQUENCE ${DataVersion}_seq;
