package web.service;

import web.entity.Task;

public interface TaskService extends IOperations<Task> {
    public Iterable<Task> findNew();
}
