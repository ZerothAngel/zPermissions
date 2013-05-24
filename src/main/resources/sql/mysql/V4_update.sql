CREATE TABLE ${Inheritance} (
  id BIGINT NOT NULL AUTO_INCREMENT,
  child_id BIGINT NOT NULL,
  parent_id BIGINT NOT NULL,
  ordering INTEGER NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (child_id, parent_id)
);
CREATE INDEX ix_${Inheritance}_child ON ${Inheritance} (child_id);
CREATE INDEX ix_${Inheritance}_parent ON ${Inheritance} (parent_id);
ALTER TABLE ${Inheritance} ADD CONSTRAINT fk_${Inheritance}_child FOREIGN KEY (child_id) REFERENCES ${PermissionEntity} (id);
ALTER TABLE ${Inheritance} ADD CONSTRAINT fk_${Inheritance}_parent FOREIGN KEY (parent_id) REFERENCES ${PermissionEntity} (id);
