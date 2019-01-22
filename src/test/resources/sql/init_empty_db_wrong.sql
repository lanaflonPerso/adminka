DROP TABLE IF EXISTS task;

DROP SEQUENCE IF EXISTS task_sequence;

CREATE SEQUENCE task_sequence
    INCREMENT 1
    START 1
    MINVALUE 1;    

CREATE TABLE task (
  id INTEGER PRIMARY KEY,
  payload VARCHAR(30),
  info  VARCHAR(10),
  status INTEGER
);

ALTER TABLE ONLY task ALTER COLUMN id SET DEFAULT nextval('task_sequence'::regclass);

ALTER DATABASE postgres SET log_statement = 'all'