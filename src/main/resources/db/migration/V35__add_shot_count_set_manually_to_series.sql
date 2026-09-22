ALTER TABLE series ADD COLUMN shot_count_set_manually BOOLEAN NOT NULL DEFAULT false;
UPDATE series SET shot_count_set_manually = true WHERE shot_count IS NOT NULL;
