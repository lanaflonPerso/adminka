package web.service;

import web.dao.TaskRepository;
import org.baeldung.web.entity.QTask;
import web.entity.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskRepository dao;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;


    @Override
    public Page<Task> findPaginated(int page, int size, String sortField, String sortDirection) {
        return dao.findAll(new PageRequest(page, size,  sortDirection.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,  sortField));
    }

    @Override
    public Iterable<Task> findNew() {
        return dao.findAll(QTask.task.status.eq(1));
    }

    @Override
    public Task insert(Task task) {
        return dao.save(task);
    }

    @Override
    public List<Task> insert(List<Task> tasks) {
        List<Task> results = dao.save(tasks);
        return results;
    }
}
