CREATE TABLE ${EntityMetadata} (
  id BIGINT NOT NULL AUTO_INCREMENT,
  entity_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  string_value VARCHAR(255),
  integer_value BIGINT,
  real_value DOUBLE PRECISION,
  boolean_value TINYINT(1),
  PRIMARY KEY (id),
  UNIQUE (entity_id, name)
);
CREATE INDEX ix_${EntityMetadata}_entity ON ${EntityMetadata} (entity_id);
ALTER TABLE ${EntityMetadata} ADD CONSTRAINT fk_${EntityMetadata}_entity FOREIGN KEY (entity_id) REFERENCES ${PermissionEntity} (id);
