CREATE TABLE ${UuidDisplayNameCache} (
  name VARCHAR(255),
  display_name VARCHAR(255) NOT NULL,
  uuid VARCHAR(255) NOT NULL,
  timestamp DATETIME NOT NULL,
  PRIMARY KEY (name)
);
