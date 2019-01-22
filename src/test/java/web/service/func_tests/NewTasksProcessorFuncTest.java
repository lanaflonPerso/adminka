package web.service.func_tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import web.components.NewTasksProcessor;
import web.entity.Task;
import web.service.ApplicationTest;
import web.service.TaskService;
import web.service.helper.QueueListener;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static web.service.helper.DbExecutor.execute;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApplicationTest.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class NewTasksProcessorFuncTest {

    @Autowired
    NewTasksProcessor newTasksProcessor;

    @Autowired
    private TaskService service;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Connection rabbitMQConnection;

    @Autowired
    ObjectMapper mapper;

   @Before
   public void createDb() throws SQLException {
       execute(dataSource, "sql/init_empty_db.sql", "sql/insert_1_row.sql");
   }

    @Test
    public void testCommit() throws IOException, InterruptedException {
       // given
        Channel channel = rabbitMQConnection.createChannel();
        BlockingQueue<String> signalQueue = QueueListener.listen(channel, "new_tasks");
        List<Task> list = new ArrayList<>();
        list.add(new Task("http://ya.ru", "", 1));
        list.add(new Task("http://mail.ru", "", 1));

        // when
        newTasksProcessor.save(list);
        String yaStr = signalQueue.poll(1, TimeUnit.SECONDS);
        String mailStr = signalQueue.poll(1, TimeUnit.SECONDS);
        String nothing = signalQueue.poll(1, TimeUnit.SECONDS);
        Assert.assertNotNull(yaStr);
        Assert.assertNotNull(mailStr);
        Assert.assertNull(nothing);
        Task ya = mapper.readValue(yaStr, Task.class);
        Task mail = mapper.readValue(mailStr, Task.class);
        Assert.assertEquals(new Task(list.get(0).getId(), "http://ya.ru", "published", 2), ya );
        Assert.assertEquals(new Task(list.get(1).getId(), "http://mail.ru", "published", 2), mail );

        Page<Task> tasks = service.findPaginated(0, 5, "id", "asc");
        Assert.assertEquals(3, tasks.getTotalElements());
        Assert.assertThat(tasks, Matchers.hasItem(new Task(list.get(0).getId(), "http://ya.ru", "published", 2)));
        Assert.assertThat(tasks, Matchers.hasItem(new Task(list.get(1).getId(), "http://mail.ru", "published", 2)));
        Assert.assertThat(tasks, Matchers.hasItem(new Task(1000, "http://google.com", "infa", 3)));



    }
}
