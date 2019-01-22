package web.main;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import web.include.Publisher;
import web.include.Receiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

@SpringBootApplication
@Import({PersistenceConfig.class, WebSocketConfig.class})
@PropertySource({"classpath:${mode:live}.properties"})
public class Application extends SpringBootServletInitializer {
    private Logger logger = LoggerFactory.getLogger(Application.class);

    @Autowired
    private Environment env;

    @Autowired
    private DataSource dataSource;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
        return new ShallowEtagHeaderFilter();
    }

    @Bean("rabbitMQConnection")
    public Connection getConnectionFactory() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(env.getProperty("queue.host"));
        factory.setPort(Integer.parseInt(env.getProperty("queue.port")));
        factory.setUsername(env.getProperty("queue.username"));
        factory.setPassword(env.getProperty("queue.password"));
        factory.setVirtualHost(env.getProperty("queue.virtualhost"));
        factory.setConnectionTimeout(0);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setTopologyRecoveryEnabled(true);
        return factory.newConnection();
    }

    @Bean("publisher")
    @Autowired
    public Publisher getPublisher(Connection connection) throws IOException {
        return new Publisher(connection, "new_tasks");
    }

    @Bean("receiver")
    @Autowired
    public Receiver getReceiver(Connection connection) throws IOException {
        return new Receiver(connection, "tasks_in_progress");
    }

    @Bean
    public ObjectMapper mapper() {
        return new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
    }

    @PostConstruct
    public void init() throws SQLException {
        java.sql.Connection db = dataSource.getConnection();
        db.setAutoCommit(false);
        ScriptUtils.executeSqlScript(db, new ClassPathResource("db/sql/postgresql_install.sql"));
        db.setAutoCommit(true);
        db.close();
        System.out.println("Database initialized");    }
}
