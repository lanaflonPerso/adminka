package web.service.end2end_tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import web.components.ResultsProcessor;
import web.entity.Task;
import web.include.Receiver;
import web.service.ApplicationTest;
import web.service.TaskService;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static web.service.helper.DbExecutor.execute;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApplicationTest.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class FullCircleTest {

    private static final String ENDPOINT_GET = "http://localhost:8080/task/get";
    private static final String ENDPOINT_SAVE = "http://localhost:8080/task/save";

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

    @Autowired
    private Receiver receiver;

    @Before
    public void configureRestAssured() {
        RestAssured.reset();
        RestAssured.filters(
            new RequestLoggingFilter(),
            new ResponseLoggingFilter());
    }

    @Before
   public void createDb() throws SQLException {
        execute(dataSource, "sql/init_empty_db.sql");
   }

    @Test
    public void testQuickProcessing() throws IOException, InterruptedException {
        // given
        // tasks` workers imitation:
        Channel channel = rabbitMQConnection.createChannel();
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                Task task = mapper.readValue(message, Task.class);
                Task updatedTask = new Task(task.getId(), task.getLink() + "_updated", task.getInfo() + "_updated", task.getStatus() + 1);
                channel.basicPublish("", "tasks_in_progress", null, mapper.writeValueAsString(updatedTask).getBytes());
            }
        };
        channel.queuePurge("new_tasks");
        channel.basicConsume("new_tasks", true, consumer);


        List<Task> list = new ArrayList<>();
        list.add(new Task("http://ya.ru", "info1", 1));
        list.add(new Task("http://mail.ru", "info2", 1));

        // when
        given()
            .contentType("application/json")
            .body(list)
        .when()
            .post(ENDPOINT_SAVE);
        Thread.sleep(5000);

        //then
        List<Task> updatedTasks =
            given()
                .param("page", "0")
                .param("size", "5")
                .param("sortField", "id")
                .param("sortDirection", "asc")
                .when()
                .get(ENDPOINT_GET)
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList("content", Task.class);

        List<Task> expectedTasks = new ArrayList<>();
        expectedTasks.add(new Task(10, "http://ya.ru_updated", "published_updated", 3));
        expectedTasks.add(new Task(11, "http://mail.ru_updated", "published_updated", 3));

        Assert.assertEquals(2, updatedTasks.size());
        for (Task updatedTask : updatedTasks) {
            Assert.assertThat(expectedTasks, Matchers.hasItem(updatedTask));
        }
    }



    @Test
    public void testSlowProcessing() throws IOException, InterruptedException {
        // given
        // tasks` workers imitation:
        Channel channel = rabbitMQConnection.createChannel();
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String message = new String(body, StandardCharsets.UTF_8);
                Task task = mapper.readValue(message, Task.class);
                Task updatedTask = new Task(task.getId(), task.getLink() + "_updated", task.getInfo() + "_updated", task.getStatus() + 1);
                channel.basicPublish("", "tasks_in_progress", null, mapper.writeValueAsString(updatedTask).getBytes());
            }
        };
        channel.queuePurge("new_tasks");
        channel.basicConsume("new_tasks", true, consumer);

        List<Task> list = new ArrayList<>();
        list.add(new Task("http://ya.ru", "info1", 1));
        list.add(new Task("http://mail.ru", "info2", 1));

        // when
        given()
            .contentType("application/json")
            .body(list)
            .when()
            .post(ENDPOINT_SAVE);
        Thread.sleep(7000);

        //then
        List<Task> updatedTasks =
            given()
                .param("page", "0")
                .param("size", "5")
                .param("sortField", "id")
                .param("sortDirection", "asc")
                .when()
                .get(ENDPOINT_GET)
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList("content", Task.class);

        List<Task> expectedTasks = new ArrayList<>();
        expectedTasks.add(new Task(10, "http://ya.ru_updated", "published_updated", 3));
        expectedTasks.add(new Task(11, "http://mail.ru_updated", "published_updated", 3));

        Assert.assertEquals(2, updatedTasks.size());
        for (Task updatedTask : updatedTasks) {
            Assert.assertThat(expectedTasks, Matchers.hasItem(updatedTask));
        }
    }
}

