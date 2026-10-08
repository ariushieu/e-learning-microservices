-- Old enrollments remain unnamed until the learner reactivates them.
ALTER TABLE enrollments ADD COLUMN learner_name VARCHAR(150) NULL;
