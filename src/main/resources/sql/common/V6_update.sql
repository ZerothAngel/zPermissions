ALTER TABLE ${Membership} ADD display_name VARCHAR(255);
UPDATE ${Membership} SET display_name = member;
ALTER TABLE ${Membership} ALTER COLUMN display_name SET NOT NULL;
