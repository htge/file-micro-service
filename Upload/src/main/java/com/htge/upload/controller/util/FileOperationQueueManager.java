package com.htge.upload.controller.util;

import org.jboss.logging.Logger;
import org.springframework.util.StopWatch;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FileOperationQueueManager {
    static final private Map<File, FileOperationQueue> fileQueue = new HashMap<>();
    static final private Lock lock = new ReentrantLock();
    static final private Logger logger = Logger.getLogger(FileOperationQueueManager.class);

    static public FileOperationQueue getInstance(File destFile) {
        lock.lock();
        FileOperationQueue queue = null;
        if (fileQueue.containsKey(destFile)) {
            queue = fileQueue.get(destFile);
        }
        lock.unlock();
        if (queue == null) {
            logger.info("queue null?");
        }
        return queue;
    }

    static public FileOperationQueue createInstance(File destFile, Long totalPart) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        lock.lock();
        FileOperationQueue queue;
        FileOperationQueue oldQueue;
        if (fileQueue.containsKey(destFile)) {
            oldQueue = fileQueue.get(destFile);
            oldQueue.interrupt();
        }
        queue = new FileOperationQueue(destFile, totalPart);
        fileQueue.put(destFile, queue);
        lock.unlock();
        stopWatch.stop();
        logger.info("createInstance elapsed = " + stopWatch.getTotalTimeMillis());
        return queue;
    }

    static void delete(File destFile) {
        lock.lock();
        fileQueue.remove(destFile);
        lock.unlock();
    }
}
