package com.htge.upload.controller.util;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

public class FileOperationQueue {
    static final private ConcurrentHashMap<File, FileOperationQueue> fileQueue = new ConcurrentHashMap<>();

    final private Object waitObject = new Object();
    private boolean isRunning = true;

    private LinkedBlockingDeque<FileItem> queue = new LinkedBlockingDeque<>();

    private FileOperationQueue(File file, Long totalPart, Long blockSize) throws IOException {

        fileQueue.put(file, this);
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException("File could not be created");
            }
        }

        final RandomAccessFile accessFile = new RandomAccessFile(file, "rw");
        final FileChannel fileChannel = accessFile.getChannel();
        final FileLock fileLock = fileChannel.lock();

        final Thread queueThread = new Thread(() -> {
            final byte[] buffer = new byte[1048576];
            InputStream inputStream;

            while (true) {
                try {
                    FileItem fileItem = queue.take();

                    //输入的文件内容转换
                    inputStream = fileItem.inputStream;

                    final long offset = (fileItem.currentPart - 1) * blockSize;
                    final long fileSize = fileItem.size;
                    final long fileLength = offset + fileSize;
//                    if (fileSize > blockSize) {
//                        object.put("error", "Package too large!");
//                        object.put("errorCode", 4);
//                        return new ResponseEntity<>(object, HttpStatus.BAD_REQUEST);
//                    }
                    long length = accessFile.length();
                    if (length < fileLength) {
                        accessFile.setLength(fileLength);
                    }
                    accessFile.seek(offset);

                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        accessFile.write(buffer, 0, len);
                    }

                    //最后一条记录
                    if (fileItem.currentPart.equals(totalPart)) {
                        fileQueue.remove(file);
                        isRunning = false;
                        synchronized (waitObject) {
                            waitObject.notifyAll();
                        }
                        break;
                    }
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                inputStream.close();
                fileLock.close();
                fileChannel.close();
                accessFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        queueThread.start();
    }

    public void addItem(FileItem item) {
        queue.add(item);
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

    public static FileOperationQueue createInstance(File destFile, Long totalPart, Long blockSize) throws IOException {
        //TODO：多客户端上传同一个文件，可能存在问题。这里要加处理
        FileOperationQueue queue = getInstance(destFile);
        if (queue == null) {
            return new FileOperationQueue(destFile, totalPart, blockSize);
        }
        return queue;
    }

    public static FileOperationQueue getInstance(File destFile) {
        if (fileQueue.containsKey(destFile)) {
            return fileQueue.get(destFile);
        }
        return null;
    }
}
