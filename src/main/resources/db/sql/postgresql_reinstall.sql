DROP TABLE IF EXISTS task;

DROP SEQUENCE IF EXISTS task_sequence;

CREATE SEQUENCE task_sequence
  INCREMENT 1
  START 1
  MINVALUE 1;

CREATE TABLE task (
  id     INTEGER PRIMARY KEY,
  payload   TEXT,
  info   TEXT,
  status INTEGER
);

ALTER TABLE ONLY task
  ALTER COLUMN id SET DEFAULT nextval('task_sequence' :: regclass);

CREATE INDEX task_id_idx ON task (id) ;
CREATE INDEX task_status_idx ON task (status) ;

-- INSERT INTO task (payload, info, status) VALUES ('http://ya.ru', '', 1);

--ALTER DATABASE postgres SET log_statement = 'all'