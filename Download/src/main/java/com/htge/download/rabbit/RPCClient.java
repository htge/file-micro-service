package com.htge.download.rabbit;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import org.jboss.logging.Logger;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;

public class RPCClient {

    private Connection connection;
    private Channel channel;
    private String replyQueueName;
    private final ConcurrentHashMap<String, BlockingQueue<byte[]>> queryInfo = new ConcurrentHashMap<>();
    private Logger logger = Logger.getLogger(RPCClient.class);

    public RPCClient(CachingConnectionFactory factory) throws IOException {
        connection = factory.createConnection();
        channel = connection.createChannel(false);

        replyQueueName = channel.queueDeclare().getQueue();
        channel.basicConsume(replyQueueName, true, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                try {
                    String id = properties.getCorrelationId();
                    logger.info("id = " + id);
                    BlockingQueue<byte[]> queue = queryInfo.get(id);
                    queue.offer(body);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public String call(String requestQueueName, String message) throws IOException, InterruptedException {
        final String corrId = UUID.randomUUID().toString();

        BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(1);
        queryInfo.put(corrId, queue);

        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();
        logger.info("corrId = "+corrId);
        channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));
        return new String(queue.take(), "UTF-8");
    }

    public void close() {
        connection.close();
    }
}