package com.htge.download.rabbit;

import com.rabbitmq.client.*;
import org.jboss.logging.Logger;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

public class RecvUploadServer {
    private static final String QUEUE_NAME = "upload_queue";
    private static Logger logger = Logger.getLogger(RecvUploadServer.class);
    private static final ReentrantLock createLock = new ReentrantLock();
    private static boolean isCreated = false;
    private CachingConnectionFactory factory = null;

    public void setFactory(CachingConnectionFactory factory) {
        this.factory = factory;
        createInstance();
    }

    private void createInstance() {
        //不允许多实例
        if (doTrick()) {
            return;
        }

        new Thread(() -> {
            Connection connection = null;
            try {
                connection = factory.createConnection();
                Channel channel = connection.createChannel(true);

                //
                channel.queueDeclare(QUEUE_NAME, true, false, false, null);

                Consumer consumer = new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        String message = new String(body, "UTF-8");
                        logger.info(" [x] Received '" + message + "'");
                    }
                };

                channel.basicConsume(QUEUE_NAME, true, consumer);
                logger.info(" [x] Awaiting '"+QUEUE_NAME+"' requests");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
        }).start();
    }

    private static boolean doTrick() {
        createLock.lock();
        boolean created = isCreated;
        isCreated = true;
        createLock.unlock();
        return created;
    }
}
