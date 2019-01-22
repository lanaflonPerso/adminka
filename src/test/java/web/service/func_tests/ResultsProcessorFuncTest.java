package web.service.func_tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import web.components.ResultsProcessor;
import web.entity.Task;
import web.service.ApplicationTest;
import web.service.TaskService;
import web.service.helper.QueueListener;
import org.hamcrest.Matchers;
import org.junit.Assert;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static web.service.helper.DbExecutor.execute;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApplicationTest.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class ResultsProcessorFuncTest {

    @Autowired
    ResultsProcessor resultsProcessor;

    @Autowired
    private TaskService service;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private Connection rabbitMQConnection;

    @Test
    public void taskResultShouldUpdateDbRaw() throws IOException, InterruptedException, SQLException {
        // given
        execute(dataSource, "sql/init_empty_db.sql", "sql/insert_3_rows.sql");
        Task processed =    new Task(1001, "http://mail.ru_changed", "changed___________________", 9999);
        Task expected = new Task(1001, "http://mail.ru_changed", "changed___________________", 3);

        // when
        Channel channel = rabbitMQConnection.createChannel();
        channel.basicPublish("", "tasks_in_progress", null, mapper.writeValueAsString(processed).getBytes());

        // then
        Thread.sleep(3000);
        Page<Task> tasks = service.findPaginated(0, 5, "id", "asc");
        Assert.assertEquals(3, tasks.getTotalElements());
        Assert.assertThat(tasks, Matchers.hasItem(expected));
    }

    @Test
    public void taskResultShouldNotBeLostIfDbErrorOccures() throws IOException, InterruptedException, SQLException {
        // given
        execute(dataSource, "sql/init_empty_db_wrong.sql", "sql/insert_3_rows.sql");
        Task processed =    new Task(1001, "http://mail.ru_changed", "changed___________________", 9999);
        Task original = new Task(1001, "http://mail.ru", "infa2", 2);

        Channel channel = rabbitMQConnection.createChannel();
        BlockingQueue<String> signalQueue = QueueListener.listen(channel, "dead_queue");
        // when
        channel.basicPublish("", "tasks_in_progress", null, mapper.writeValueAsString(processed).getBytes());

        // then
        String result = signalQueue.poll(5, TimeUnit.SECONDS);
        Assert.assertNotNull(result);
        Assert.assertEquals(processed, mapper.readValue(result, Task.class));
        Page<Task> tasks = service.findPaginated(0, 5, "id", "asc");
        Assert.assertEquals(3, tasks.getTotalElements());
        Assert.assertThat(tasks, Matchers.hasItem(original));
    }
}
