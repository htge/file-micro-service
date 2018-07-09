package com.htge.download.file.cache;

import org.jboss.logging.Logger;
import org.springframework.util.StopWatch;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileETagGeneration<T> {
    private final Map<T, ReadWriteLock> generateMap = new HashMap<>();
    private final Lock mapLock = new ReentrantLock();
    private final Logger logger = Logger.getLogger(FileETagGeneration.class);

    String generate(T file, FileETagGenerationCallback<T> callback) {
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        ReadWriteLock lock;
        boolean isLock;
        String ETag = null;

        mapLock.lock();
        lock = generateMap.get(file);
        isLock = (lock != null);
        if (!isLock) {
            lock = new ReentrantReadWriteLock();
            generateMap.put(file, lock);
        }
        mapLock.unlock();

        //先生成
        if (!isLock) {
            lock.writeLock().lock();
            ETag = callback.generateETag(file);
            lock.writeLock().unlock();
        } else {
            stopWatch.stop();
            logger.info("file is generating, do read, check elapsed = "+stopWatch.getTotalTimeMillis() + "ms");
        }

        //后读取
//        boolean isETag = ETag != null;
        lock.readLock().lock();
        ETag = callback.afterGenerated(ETag);
        lock.readLock().unlock();
//        if (!isETag) {
//            logger.info("file "+file.toString()+" eTag loaded: "+ETag);
//        }
        return ETag;
    }

    public interface FileETagGenerationCallback<T> {
        String generateETag(T file);
        String afterGenerated(String ETag);
    }
}
