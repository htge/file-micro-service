package com.htge.login.rabbit;

import com.rabbitmq.client.*;
import org.apache.catalina.LifecycleException;
import org.jboss.logging.Logger;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;

import java.io.IOException;
import java.net.BindException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

public class RPCServer {
    @SuppressWarnings("FieldCanBeLocal")
    private static Logger logger = Logger.getLogger(RPCServer.class);
    private static final ReentrantLock createLock = new ReentrantLock();
    private static boolean isCreated = false;
    private static boolean isRunning = true;

    private Channel channel = null;
    private Connection connection = null;
    private Consumer consumer = null;

    private CachingConnectionFactory factory = null;
    private RPCData rpcData = null;
    private String queueName = null;

    public void setFactory(CachingConnectionFactory factory) {
        this.factory = factory;
    }

    public void setRpcData(RPCData rpcData) {
        this.rpcData = rpcData;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    @SuppressWarnings({"Convert2MethodRef", "CodeBlock2Expr"})
    public void startServer() {
        //不允许多实例
        if (doTrick()) {
            return;
        }

        new Thread(()-> {
            while (isRunning) {
                try {
                    runServer();
                } catch (IllegalStateException e) {
                    //出现了无法恢复的异常
                    e.printStackTrace();
                    break;
                } catch (IOException e) {
                    //连接遇到意外关闭的情况，恢复
                    if (e.getCause() instanceof ShutdownSignalException) {
                        logger.info("restart RPC server...");
                    } else {
                        e.printStackTrace();
                        break;
                    }
                } catch (AmqpConnectException e) {
                    try {
                        logger.info("connecting RPC server...");
                        cleanup();
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    cleanup();
                }
            }
        }).start();
        new RPCThreadHook(() -> {
            isRunning = false;
            cleanup();
        });
    }

    private void cleanup() {
        closeChannel();
        closeConnection();
    }

    private void closeChannel() {
        if (channel != null) {
            try {
                channel.close();
                channel = null;
            } catch (TimeoutException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeConnection() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    private void runServer() throws IOException {
        final Object waitObject = new Object(); //有异常的时候会需要重建，无异常的话会始终阻塞
        final int consumerCount = 5; //消费者个数，并行量
        connection = factory.createConnection();
        channel = connection.createChannel(false);

        for (int i=0; i<2; i++) {
            try {
                channel.queueDeclare(queueName, false, false, false, null);
            } catch (Exception e) {
                channel.queueDelete(queueName);
            }
        }

        channel.basicQos(1);

        logger.info(" [x] Awaiting RPC requests");

        while (channel != null && channel.isOpen()) {
            if (consumer == null) {
                logger.info("Creating default consumer...");
                consumer = new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        //收到事件后马上批量建立消费者，直到满足需求为止
                        while (channel.consumerCount(queueName) <= consumerCount) {
                            channel.basicConsume(queueName, false, consumer);
                        }

                        //处理事件
                        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                                .Builder()
                                .correlationId(properties.getCorrelationId())
                                .build();

                        String response = "";

                        try {
                            response = rpcData.parseData(new String(body, "UTF-8"));
                            logger.info("RPC Response: consumerTag = "+consumerTag+" corrId = " + properties.getCorrelationId() + "\nresponse = " + response);
                        } catch (Exception e) {
                            logger.error(" [.] " + e.toString());
                        } finally {
                            if (response != null) {
                                channel.basicPublish("", properties.getReplyTo(), replyProps, response.getBytes("UTF-8"));
                                logger.info("basicAck: consumerTag = "+envelope.getDeliveryTag());
                                channel.basicAck(envelope.getDeliveryTag(), false);
                                //无用的consumer，必须释放，否则可能导致服务资源占满
                                channel.basicCancel(consumerTag);
                            }
                        }
                    }

                    @Override
                    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                        logger.warn("handleShutdownSignal, resume...");
                        //RabbitMQ服务重启后，先清理资源再不断重试，直到连接成功
                        synchronized (waitObject) {
                            consumer = null;
                            cleanup();
                            waitObject.notifyAll();
                        }
                        super.handleShutdownSignal(consumerTag, sig);
                    }
                };
            }

            //批量建立消费者，直到满足需求为止
            while (channel.consumerCount(queueName) < consumerCount) {
                channel.basicConsume(queueName, false, consumer);
            }
            synchronized (waitObject) {
                try {
                    waitObject.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean doTrick() {
        createLock.lock();
        boolean created = isCreated;
        isCreated = true;
        createLock.unlock();
        return created;
    }

    private interface RPCThreadListener {
        void cleanup();
    }

    private class RPCThreadHook extends Thread {
        private RPCThreadListener listener;

        private RPCThreadHook(RPCThreadListener listerer) {
            this.listener = listerer;
            Runtime.getRuntime().addShutdownHook(this);
        }

        @Override
        public void run() {
            logger.info("clean RPC resource...");
            listener.cleanup();
        }
    }
}
