package web.service.rest_tests;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import web.entity.Task;
import web.include.Publisher;
import web.service.ApplicationTest;
import web.service.TaskService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.domain.Page;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static web.service.helper.DbExecutor.execute;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApplicationTest.class, webEnvironment = WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class HttpRestTests {

    private static final String ENDPOINT_GET = "http://localhost:8080/task/get";
    private static final String ENDPOINT_SAVE = "http://localhost:8080/task/save";


    @Autowired
    Publisher publisher;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TaskService service;

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
    public void save2Items() throws InterruptedException {
        // given
        List<Task> list = new ArrayList<>();
        list.add(new Task("http://ya.ru", "info1", 1));
        list.add(new Task("http://mail.ru", "info2", 1));

        // when
        given()
            .contentType("application/json")
            .body(list)
            .when()
            .post(ENDPOINT_SAVE)
            .then()
            .statusCode(HTTP_OK);

        //then
        Page<Task> tasks = service.findPaginated(0, 5, "id", "asc");
        Assert.assertEquals(2, tasks.getTotalElements());
    }

    @Test
    public void list2ItemsPerPage() throws SQLException {

        execute(dataSource, "sql/insert_3_rows.sql");

        List<Task> tasks =
        given()
            .param("page", "0")
            .param("size", "2")
            .param("sortField", "id")
            .param("sortDirection", "asc")
        .when()
            .get(ENDPOINT_GET)
        .then()
            .assertThat()
            .body("size", is(2))
            .body("totalElements", is(3))
            .body("totalPages", is(2))
            .body("numberOfElements", is(2))
        .extract()
            .body()
            .jsonPath()
            .getList("content", Task.class);

        Assert.assertEquals(2, tasks.size());
        Assert.assertEquals(new Task(1000, "http://ya.ru", "infa1", 1), tasks.get(0));
        Assert.assertEquals(new Task(1001, "http://mail.ru", "infa2", 2), tasks.get(1));
    }

    @Test
    public void list2ItemsPerPageReversed() throws SQLException {

        execute(dataSource, "sql/init_empty_db.sql", "sql/insert_3_rows.sql");

        List<Task> tasks =
            given()
                .param("page", "0")
                .param("size", "2")
                .param("sortField", "id")
                .param("sortDirection", "desc")
            .when()
                .get(ENDPOINT_GET)
            .then()
                .assertThat()
                .body("size", is(2))
                .body("totalElements", is(3))
                .body("totalPages", is(2))
                .body("numberOfElements", is(2))
            .extract()
                .body()
                .jsonPath()
                .getList("content", Task.class);

        Assert.assertEquals(2, tasks.size());
        Assert.assertEquals(new Task(1002, "http://google.com", "infa3", 3), tasks.get(0));
        Assert.assertEquals(new Task(1001, "http://mail.ru", "infa2", 2), tasks.get(1));
    }
}
