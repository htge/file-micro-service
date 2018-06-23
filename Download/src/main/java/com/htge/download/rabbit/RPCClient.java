package com.htge.download.rabbit;

import com.rabbitmq.client.*;
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
    private final DefaultConsumer consumer = new DefaultConsumer(channel) {
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
    };

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
            } catch (IOException e) {
                logger.warn("io error?");
                e.printStackTrace();
            }
        }

        final String corrId = UUID.randomUUID().toString();

        BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(1);
        queryInfo.put(corrId, queue);

        if (connection.isOpen()) {
            try {
                //basicConsume不会所有时间都能收到数据，所以需要每次请求的时候都设置一次
                channel.basicConsume(replyQueueName, true, consumer);

                //发布后，200ms超时，超时后重试
                AMQP.BasicProperties props = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(corrId)
                        .replyTo(replyQueueName)
                        .build();
                logger.info("request id = "+corrId);
                channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));
                byte[] polled = queue.poll(200, TimeUnit.MILLISECONDS);
                if (polled != null) {
                    return new String(polled, "UTF-8");
                } else {
                    logger.warn("timeout?");
                    return retry(requestQueueName, message, n+1, limit);
                }
            } catch (IOException | InterruptedException e) {
                //遇到shutdown的错误，尝试自动恢复
                if (e.getCause() instanceof ShutdownSignalException) {
                    logger.warn("channel shutdown, retry...");
                    connection.close();
                    channel = null;
                    return retry(requestQueueName, message, n+1, limit);
                }
                e.printStackTrace();
            }
            return null;
        }
        logger.warn("connection closed?");
        channel = null;
        return retry(requestQueueName, message, n+1, limit);
    }

    public String call(String requestQueueName, String message) {
        return retry(requestQueueName, message, 0, 3);
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
        channel = null;
    }
}