package web.include;

import com.rabbitmq.client.*;
import web.components.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Receiver {
    private Logger logger = LoggerFactory.getLogger(Receiver.class);
    private List<MessageHandler> handlers = new ArrayList<>();

    public Receiver(Connection connection, String queue) throws IOException {
        Channel channel = connection.createChannel();

        channel.exchangeDeclare("dead", "fanout");
        channel.queueDeclare("dead_queue", true, false, false, null);
        channel.queueBind("dead_queue", "dead", "");
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "dead");

        channel.queueDeclare(queue, true, false, false, args);
        logger.trace(queue + " queue initialized");

        channel.basicQos(1);


        channel.basicConsume(queue, false, new Consumer(channel));
    }

    public void addHandler(MessageHandler handler) {
        handlers.add(handler);
    }

    class Consumer extends DefaultConsumer {

        Consumer(Channel channel) {
            super(channel);
        }

        @Override
        public void handleDelivery(String consumerTag,
                                   Envelope envelope,
                                   AMQP.BasicProperties properties,
                                   byte[] body) throws IOException {
            String message = new String(body, StandardCharsets.UTF_8);
            try {
                for (MessageHandler messageHandler : handlers) {
                    messageHandler.handle(message);
                }
                getChannel().basicAck(envelope.getDeliveryTag(), false);
            }
            catch (Exception e) {
                getChannel().basicNack(envelope.getDeliveryTag(), false, false);
            }
        }
    }
}
