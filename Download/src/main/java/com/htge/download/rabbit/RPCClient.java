package com.htge.download.rabbit;

import com.rabbitmq.client.*;
import org.jboss.logging.Logger;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;

public class RPCClient {
    private CachingConnectionFactory factory;

    private Connection connection = null;
    private Channel channel = null;
    private Consumer consumer = null;

    private String replyQueueName;
    private final ConcurrentHashMap<String, BlockingQueue<byte[]>> queryInfo = new ConcurrentHashMap<>();
    private Logger logger = Logger.getLogger(RPCClient.class);

    public RPCClient() {
        new ShutdownHookThread(this);
    }

    public void setFactory(CachingConnectionFactory factory) {
        this.factory = factory;
    }

    private String retry(String requestQueueName, String message) {
        //有条件重试的循环
        for (int i=0; i<3; i++) {
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

            if (connection != null && channel != null && connection.isOpen()) {
                try {
                    if (consumer == null) {
                        //basicConsume不会所有时间都能收到数据，所以需要每次请求的时候都设置一次
                        consumer = new DefaultConsumer(channel) {
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

                            @Override
                            public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                                logger.warn("handleShutdownSignal, resume...");
                                //RabbitMQ服务重启后，先清理资源再重试，会出现多次失败的情况
                                try {
                                    close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                super.handleShutdownSignal(consumerTag, sig);
                            }
                        };
                        channel.basicConsume(replyQueueName, true, consumer);
                    }

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
                        continue;
                    }
                } catch (IOException | InterruptedException e) {
                    //遇到shutdown的错误，尝试自动恢复
                    if (e.getCause() instanceof ShutdownSignalException) {
                        logger.warn("channel shutdown, retry...");
                        try {
                            close();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        continue;
                    }
                    e.printStackTrace();
                }
                //不可恢复的错误，不重试
                return null;
            }
            logger.warn("connection closed?");
            try {
                close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        //超过重试次数，不重试
        return null;
    }

    public String call(String requestQueueName, String message) {
        try {
            return retry(requestQueueName, message);
        } catch (AmqpConnectException e) {
            //连接中断
            logger.info(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private class ShutdownHookThread extends Thread {
        RPCClient rpcClient;
        private ShutdownHookThread(RPCClient rpcClient) {
            this.rpcClient = rpcClient;
            Runtime.getRuntime().addShutdownHook(this);
        }

        @Override
        public void run() {
            try {
                logger.info("Close connection...");
                rpcClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void close() throws IOException, TimeoutException {
        consumer = null;
        if (channel != null) {
            channel.close();
            channel = null;
        }
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }
}