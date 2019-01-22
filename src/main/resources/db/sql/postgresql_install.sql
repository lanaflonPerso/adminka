--ALTER DATABASE postgres SET log_statement = 'all';

CREATE SEQUENCE IF NOT EXISTS  task_sequence
  INCREMENT 1
  START 1
  MINVALUE 1;

CREATE TABLE IF NOT EXISTS task (
  id     INTEGER PRIMARY KEY,
  payload   TEXT,
  info   TEXT,
  status INTEGER
);

CREATE INDEX IF NOT EXISTS task_id_idx ON task (id) ;
CREATE INDEX IF NOT EXISTS task_status_idx ON task (status) ;

ALTER TABLE ONLY task
  ALTER COLUMN id SET DEFAULT nextval('task_sequence' :: regclass);

-- INSERT INTO task (payload, info, status) VALUES ('http://ya.ru', '', 1);

