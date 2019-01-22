package web.rest;

import web.components.NewTasksProcessor;
import web.entity.Task;
import web.exception.MyResourceNotFoundException;
import web.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
public class TaskRestController {

    private final TaskService service;

    private final NewTasksProcessor newTasksProcessor;
    private final SimpMessageSendingOperations messagingTemplate;


    @Autowired
    public TaskRestController(TaskService service, NewTasksProcessor newTasksProcessor, SimpMessageSendingOperations messagingTemplate) {
        this.service = service;
        this.newTasksProcessor = newTasksProcessor;
        this.messagingTemplate = messagingTemplate;
    }

    @RequestMapping(value = "task/get", params = {"page", "size", "sortField", "sortDirection"}, method = RequestMethod.GET, produces = "application/json")
    public Page<Task> findPaginated(@RequestParam("page") int page, @RequestParam("size") int size, @RequestParam("sortField") String sortField, @RequestParam("sortDirection") String sortDirection) {

        Page<Task> resultPage = service.findPaginated(page, size, sortField, sortDirection);
        if (page > resultPage.getTotalPages()) {
            throw new MyResourceNotFoundException();
        }
        return resultPage;
    }

    @RequestMapping(value = "task/save", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public List<Task> save(@RequestBody List<Task> tasks) throws IOException {
        return newTasksProcessor.save(tasks);
    }

    @RequestMapping(value = "task/notify", method = RequestMethod.GET)
    public void notif() {
        messagingTemplate.convertAndSend("/topic/notify", "{}");
    }
}
