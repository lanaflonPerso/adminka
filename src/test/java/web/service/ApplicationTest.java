package web.service;

import com.rabbitmq.client.Connection;
import web.include.Publisher;
import web.main.Application;
import web.main.PersistenceConfig;
import web.main.WebSocketConfig;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.io.IOException;

@SpringBootApplication
@Import({PersistenceConfig.class, WebSocketConfig.class})
public class ApplicationTest extends Application {
    private Logger logger = LoggerFactory.getLogger(ApplicationTest.class);

    @Bean("publisher")
    @Autowired
    @Override
    public Publisher getPublisher(Connection connection) throws IOException {
        return Mockito.spy(super.getPublisher(connection));
    }
}
