ALTER TABLE ${Membership} ADD display_name VARCHAR(255);
UPDATE ${Membership} SET display_name = member;
ALTER TABLE ${Membership} CHANGE display_name display_name VARCHAR(255) NOT NULL;
