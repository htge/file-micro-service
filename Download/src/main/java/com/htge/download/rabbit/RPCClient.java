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
    private CachingConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    private String replyQueueName;
    private final ConcurrentHashMap<String, BlockingQueue<byte[]>> queryInfo = new ConcurrentHashMap<>();
    private Logger logger = Logger.getLogger(RPCClient.class);

    public RPCClient() {
        new ShutdownHookThread(this);
    }

    public void setFactory(CachingConnectionFactory factory) {
        this.factory = factory;
    }

    private String retry(String requestQueueName, String message, int n, int limit) {
        if(n >= limit) return null;
        if (channel == null) {
            connection = factory.createConnection();
            channel = connection.createChannel(false);
            try {
                replyQueueName = channel.queueDeclare().getQueue();
                channel.basicConsume(replyQueueName, true, new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                        try {
                            String id = properties.getCorrelationId();
                            logger.info("response id = " + id);
                            BlockingQueue<byte[]> queue = queryInfo.get(id);
                            queue.offer(body);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (IOException e) {
                logger.warn("io error?");
                e.printStackTrace();
            }
        }

        final String corrId = UUID.randomUUID().toString();

        BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(1);
        queryInfo.put(corrId, queue);

        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();
        logger.info("corrId = "+corrId);
        if (connection.isOpen()) {
            try {
                channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));
                //200ms超时
                byte[] polled = queue.poll(200, TimeUnit.MILLISECONDS);
                if (polled != null) {
                    return new String(polled, "UTF-8");
                } else {
                    logger.warn("timeout?");
                    return retry(requestQueueName, message, n+1, limit);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
        logger.warn("connection closed?");
        channel = null;
        return retry(requestQueueName, message, n+1, limit);
    }

    public String call(String requestQueueName, String message) {
        return retry(requestQueueName, message, 0, 5);
    }

    private class ShutdownHookThread extends Thread {
        RPCClient rpcClient;
        private ShutdownHookThread(RPCClient rpcClient) {
            this.rpcClient = rpcClient;
            Runtime.getRuntime().addShutdownHook(this);
        }

        @Override
        public void run() {
            rpcClient.close();
        }
    }

    private void close() {
        logger.info("Close connection...");
        connection.close();
    }
}