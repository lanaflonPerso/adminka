package web.service.func_tests;

import com.google.common.collect.Lists;
import web.entity.Task;
import web.service.ApplicationTest;
import web.service.TaskService;
import org.hamcrest.Matchers;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static web.service.helper.DbExecutor.execute;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApplicationTest.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class TaskServiceFuncTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TaskService service;

    @Autowired
    private DataSource dataSource;

    @Before
    public void createDb() throws SQLException {
        execute(dataSource, "sql/init_empty_db.sql", "sql/insert_3_rows.sql");
    }

    @Test
    public void insertTest() {
        //when
        Task task = service.insert(new Task("single", "_", 1));

        //then
        Page<Task> tasks = service.findPaginated(0, 5, "id", "asc");
        Assert.assertEquals(4, tasks.getTotalElements());
        Assert.assertThat(tasks, Matchers.hasItem(task));
    }

    @Test
    public void batchInsertTest() {
        // given
        List<Task> list = new ArrayList<>();
        for (int i = 0; i < 2; i ++) {
            list.add(new Task("batch_" + i, "info_" + i, i));
        }

        //when
        service.insert(list);
        Page<Task> tasks = service.findPaginated(0, 5, "id", "asc");
        Assert.assertEquals(5, tasks.getTotalElements());

        //then
        for (Task expected: list) {
            Assert.assertThat(tasks, Matchers.hasItem(expected));
        }
    }


    @Test
    public void updateTest() {
        // given
        List<Task> updatedTasks = new ArrayList<>();
        updatedTasks.add(new Task(1000, "ya.ru_changed", "_changed", 50));
        updatedTasks.add(new Task(1001, "mail.ru_changed", "_changed", 60));
        updatedTasks.add(new Task(1002, "google.ru_changed", "_changed", 70));

        // when
        service.insert(updatedTasks);

        // then
        Page<Task> tasks = service.findPaginated(0, 5, "id", "asc");
        Assert.assertEquals(3, tasks.getTotalElements());
        for (Task expected: updatedTasks) {
            Assert.assertThat(tasks, Matchers.hasItem(expected));
        }
    }

   // @Test
    public void updateBatchTest() {
        List<Task> updatedTasks = new ArrayList<>();
        updatedTasks.add(new Task(1000, "ya_.ru", "+", 5));
        updatedTasks.add(new Task(1001, "mail_.ru", "+", 6));
        updatedTasks.add(new Task(1002, "google_.ru", "+", 7));

        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();

        String ids = String.join(",", updatedTasks.stream().map(task -> task.getId().toString()).collect(Collectors.toList()));
        String links = String.join(",", updatedTasks.stream().map(task -> "'" + task.getLink() + "'").collect(Collectors.toList()));
        String infos = String.join(",", updatedTasks.stream().map(task -> "'" + task.getInfo() + "'").collect(Collectors.toList()));
        String statuses = String.join(",", updatedTasks.stream().map(task -> task.getStatus().toString()).collect(Collectors.toList()));
        Query query = session.createSQLQuery(
            "update task set info = tmp_table.info\n" +
                "from\n" +
                "(select unnest(array[" + ids + "]) as id," +
                "unnest(array[" + links + "]) as payload," +
                "unnest(array[" + infos + "]) as info," +
                "unnest(array[" + statuses + "]) as status) as tmp_table\n" +
                "where task.id = tmp_table.id;");
        query.executeUpdate();

        tx.commit();
        session.close();
    }

    @Test
    public void selectTest() {
        Task expected = new Task(1000, "http://ya.ru", "infa1", 1);
        List<Task> result = Lists.newArrayList(service.findNew());
        Assert.assertEquals(1, result.size());
        Assert.assertThat(result, Matchers.hasItem(expected));
    }

    @Test
    public void conditionalUpdatePositiveTest() {
        //given
        Task task = new Task(1000, "http://ya.ru", "infa1", 2);

        //when
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Session session = sessionFactory.openSession();
        Query query = session.createQuery("UPDATE Task SET status = :status WHERE status = 1 AND id = :id");
        query.setParameter("id", task.getId());
        query.setParameter("status", task.getStatus());
        query.executeUpdate();

        //then
        Page<Task> tasks = service.findPaginated(0, 5, "id", "asc");
        Assert.assertThat(tasks, Matchers.hasItem(task));
    }

    @Test
    public void conditionalUpdateNegativeTest() {
        //given
        Task task = new Task(1002, "http://google.com", "infa3", 2);

        //when
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Session session = sessionFactory.openSession();
        Query query = session.createQuery("UPDATE Task SET status = :status WHERE status = 1 AND id = :id");
        query.setParameter("id", task.getId());
        query.setParameter("status", task.getStatus());
        query.executeUpdate();

        //then
        Page<Task> tasks = service.findPaginated(0, 5, "id", "asc");
        Assert.assertThat(tasks, Matchers.not(Matchers.hasItem(task)));
        Assert.assertThat(tasks, (Matchers.hasItem(new Task(1002, "http://google.com", "infa3", 3))));
    }
}
