package web.components;


import com.fasterxml.jackson.databind.ObjectMapper;
import web.entity.Task;
import web.include.Receiver;
import web.service.TaskService;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.io.IOException;

@Component
@DependsOn("receiver")
public class ResultsProcessor implements MessageHandler {

    private final TaskService service;
    private final ObjectMapper mapper;
    private final EntityManager entityManager;
    private SimpMessageSendingOperations messagingTemplate;


    @Autowired
    public ResultsProcessor(Receiver receiver, TaskService service, ObjectMapper mapper, EntityManager entityManager, SimpMessageSendingOperations messagingTemplate) {
        receiver.addHandler(this);
        this.service = service;
        this.mapper = mapper;
        this.entityManager = entityManager;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void handle(String message) throws IOException {
        try {
            SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
            StatelessSession session = sessionFactory.openStatelessSession();
            Transaction tx = session.beginTransaction();

            Task task = mapper.readValue(message, Task.class);
            task.setStatus(3);
            session.update(task);

            tx.commit();
            session.close();
            messagingTemplate.convertAndSend( "/topic/notify", "{}");
        } catch (Exception e) {
            e.printStackTrace();
            throw e ;
        }
    }
}
