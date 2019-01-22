package web.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import web.entity.Task;
import web.include.Publisher;
import web.service.TaskService;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.util.List;

@Component
@DependsOn("publisher")
public class NewTasksProcessor {

    private final TaskService taskService;

    private final ObjectMapper mapper;

    private final Publisher publisher;

    private final EntityManager entityManager;

    private volatile Boolean checkRequired = true;

    private final Object notifier = new Object();

    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    public NewTasksProcessor(Publisher publisher, TaskService taskService, ObjectMapper mapper, EntityManager entityManager, SimpMessageSendingOperations messagingTemplate) {
        this.publisher = publisher;
        this.taskService = taskService;
        this.mapper = mapper;
        this.entityManager = entityManager;
        this.messagingTemplate = messagingTemplate;
    }

    public List<Task> save(List<Task> tasks) {
        taskService.insert(tasks);
        synchronized(notifier) {
            checkRequired = true;
            notifier.notify();
        }
        return tasks;
    }

    @PostConstruct
    public void startPublisher() {
        
        new Thread(() -> {
            while (true) {
                synchronized (notifier) {
                    while (!checkRequired) {
                        try {
                            notifier.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    checkRequired = false;
                }
                SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
                Session session = sessionFactory.openSession();
                try {
                    for (Task task : taskService.findNew()) {
                        try {
                            publisher.publish(mapper.writeValueAsString(task));
                            task.setStatus(2);
                        } catch (Exception e) {
                            task.setStatus(1);
                            task.setInfo("not published(" + e.getMessage() + ")");
                            checkRequired = true;
                        }
                        Query query = session.createQuery("UPDATE Task SET status = :status, info = :info WHERE status = 1 AND id = :id");
                        query.setParameter("id", task.getId());
                        query.setParameter("status", task.getStatus());
                        query.setParameter("info", task.getInfo());
                        query.executeUpdate();
                        sleep(100);
                    }
                    session.close();
                    messagingTemplate.convertAndSend("/topic/notify", "{}");
                    sleep(5 * 1000);
                }
                catch (InvalidDataAccessResourceUsageException | CannotCreateTransactionException e) {
                    e.printStackTrace();
                    sleep(10 * 1000);
                    checkRequired = true;
                }
            }
        }).start();
    }

    private void sleep(Integer millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
