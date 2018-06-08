package com.htge.login.rabbit;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import org.jboss.logging.Logger;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

public class RPCServer {

    private static final String RPC_QUEUE_NAME = "login_queue";
    private static final Logger logger = Logger.getLogger(RPCServer.class);
    private static final ReentrantLock createLock = new ReentrantLock();
    private static boolean isCreated = false;

    public static void createInstance(CachingConnectionFactory factory, RPCData rpcData) {
        //不允许多实例
        if (doTrick()) {
            return;
        }

        Runnable task = () -> {
            Connection connection = null;
            try {
                connection = factory.createConnection();
                final Channel channel = connection.createChannel(false);

                channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);

                channel.basicQos(1);

                logger.info(" [x] Awaiting RPC requests");

                //创建了永久的channel，直到程序退出才会释放，handleDelivery会一直处理请求
                final Consumer consumer = new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                                .Builder()
                                .correlationId(properties.getCorrelationId())
                                .build();
                        logger.info("corrId = "+properties.getCorrelationId());

                        String response = "";

                        try {
                            response = rpcData.parseData(new String(body, "UTF-8"));
                        }
                        catch (Exception e){
                            logger.error(" [.] " + e.toString());
                        }
                        finally {
                            channel.basicPublish( "", properties.getReplyTo(), replyProps, response.getBytes("UTF-8"));
                            channel.basicAck(envelope.getDeliveryTag(), false);
                            // RabbitMq consumer worker thread notifies the RPC server owner thread
                            synchronized(this) {
                                this.notify();
                            }
                        }
                    }
                };

                channel.basicConsume(RPC_QUEUE_NAME, false, consumer);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
        };
        new Thread(task).start();
    }

    private static boolean doTrick() {
        createLock.lock();
        boolean created = isCreated;
        isCreated = true;
        createLock.unlock();
        return created;
    }
}
