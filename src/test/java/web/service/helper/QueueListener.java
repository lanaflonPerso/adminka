package web.service.helper;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class QueueListener {
    public static BlockingQueue<String> listen(Channel channel, String queue) throws IOException {
        BlockingQueue<String> signalQueue = new LinkedBlockingQueue<>();
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                try {
                    signalQueue.put(message);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        channel.queuePurge(queue);
        channel.basicConsume(queue, true, consumer);
        return signalQueue;
    }
}
