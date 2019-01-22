package web.include;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Publisher {
    private Logger logger = LoggerFactory.getLogger(Publisher.class);
    private final Channel channel;
    private final String queue;

    public Publisher(Connection connection, String queue) throws IOException {
        this.queue = queue;
        channel = connection.createChannel();
        channel.queueDeclare(queue, true, false, false, null);
        logger.trace(queue + " queue initialized");
    }

    public void publish(String message) throws IOException {
        channel.basicPublish("", queue, MessageProperties.PERSISTENT_TEXT_PLAIN,message.getBytes());
    }

    public void close() throws IOException, TimeoutException {
        channel.close();
    }
}
