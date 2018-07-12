package com.htge.upload.controller.util;

import org.jboss.logging.Logger;

import java.io.*;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.MessageDigest;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class FileOperationQueue {
    final private Logger logger = Logger.getLogger(FileOperationQueue.class);
    final private Object waitObject = new Object();
    private boolean isRunning = true;
    private boolean isInterrupted = false;
    private String MD5 = null;

    private LinkedBlockingDeque<FileItem> queue = new LinkedBlockingDeque<>();

    FileOperationQueue(File file, Long totalPart) {
        RandomAccessFile randomAccessFile = null;
        FileChannel fileChannel = null;
        FileLock fileLock = null;
        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw new IOException("File could not be created");
                }
            }

            //文件相关的变量，在catch后要释放，或者在线程执行完成后释放
            randomAccessFile = new RandomAccessFile(file, "rw");
            fileChannel = randomAccessFile.getChannel();
            fileLock = fileChannel.lock();

            final FileOperationThread queueThread = new FileOperationThread();
            queueThread.setFile(file);
            queueThread.setTotalPart(totalPart);
            queueThread.setAccessFile(randomAccessFile);
            queueThread.setChannel(fileChannel);
            queueThread.setLock(fileLock);
            queueThread.setMessageDigest(MessageDigest.getInstance("md5"));
            logger.info("file resource created");
            queueThread.start();
        } catch (Exception e) {
            try {
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
                if (fileChannel != null) {
                    fileChannel.close();
                }
                if (fileLock != null) {
                    fileLock.close();
                }
                logger.info("file resource cleaned");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    void interrupt() {
        isInterrupted = true;
        queue.add(new FileItem());
    }

    public void addItem(FileItem item) {
        if (isRunning && !isInterrupted) {
            queue.add(item);
        } else {
            try {
                if (item.inputStream != null) {
                    item.inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void waitAll() {
        if (isRunning) {
            synchronized (waitObject) {
                try {
                    waitObject.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getMD5() {
        return MD5;
    }

    private class FileOperationThread extends Thread {
        private File file;
        private Long totalPart;
        private RandomAccessFile accessFile;
        private FileChannel channel;
        private FileLock lock;
        private MessageDigest messageDigest;

        private void setFile(File file) {
            this.file = file;
        }

        private void setTotalPart(Long totalPart) {
            this.totalPart = totalPart;
        }

        private void setAccessFile(RandomAccessFile accessFile) {
            this.accessFile = accessFile;
        }

        private void setChannel(FileChannel channel) {
            this.channel = channel;
        }

        private void setLock(FileLock lock) {
            this.lock = lock;
        }

        private void setMessageDigest(MessageDigest messageDigest) {
            this.messageDigest = messageDigest;
        }

        @Override
        public void run() {
            final int MBytes = 1048576;
            final byte[] buffer = new byte[MBytes];
            final int allocFileSize = 32*MBytes;
            InputStream inputStream = null;
            long currentIndex = 0;

            while (true) {
                try {
                    //网速低于6.83KB/s就别上传了
                    FileItem fileItem = queue.poll(5, TimeUnit.MINUTES);

                    //超时了，清理
                    if (fileItem == null) break;

                    //通过interrupted()创建的对象，清理
                    if (isInterrupted) break;

                    //忽略无序的信息
                    if (fileItem.currentPart != currentIndex) {
                        logger.info("wrong packet, ignore...");
                        continue;
                    }
                    currentIndex = fileItem.currentPart + 1;

                    //输入的文件内容转换
                    inputStream = fileItem.inputStream;

                    final long offset = fileItem.offset;
                    final long fileSize = fileItem.size;
                    final long fileLength = offset + fileSize;

                    long length = accessFile.length();
                    //之前的文件太小，扩大
                    if (length < fileLength) {
                        double fileBlock = fileLength;
                        fileBlock /= allocFileSize;
                        long newBlock = (long)fileBlock;
                        if (fileBlock % 1 != 0) {
                            newBlock++;
                        }
                        final long newLength = newBlock*allocFileSize;
                        logger.info("alloc block = "+newBlock);
                        accessFile.setLength(newLength);
                    }
                    accessFile.seek(offset);

                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        messageDigest.update(buffer, 0, len);
                        accessFile.write(buffer, 0, len);
                    }

                    //最后一条记录
                    logger.info("index: " + fileItem.currentPart + " total: " + totalPart);
                    if (fileItem.currentPart + 1 == totalPart) {
                        //之前的文件太大，缩小
                        if (length > fileLength) {
                            accessFile.setLength(fileLength);
                        }

                        final BigInteger integer = new BigInteger(1, messageDigest.digest());
                        MD5 = String.format("%032x", integer);
                        break;
                    }
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            cleanup();
            updateStatus();
        }

        private void cleanup() {
            try {
                lock.close();
                channel.close();
                accessFile.close();
                logger.info("file resource cleaned");
                while (queue.size() != 0) {
                    FileItem fileItem = queue.take();
                    if (fileItem.inputStream != null) {
                        fileItem.inputStream.close();
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void updateStatus() {
            //interrupt()，不能执行删除，会死锁
            if (!isInterrupted) {
                FileOperationQueueManager.delete(file);
            }
            isRunning = false;
            synchronized (waitObject) {
                waitObject.notifyAll();
            }
        }
    }
}
