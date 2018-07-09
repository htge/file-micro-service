package com.htge.download.file.cache;

import com.htge.download.config.FileProperties;
import com.htge.download.file.util.FileHash;
import org.jboss.logging.Logger;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FileETagCache extends FileETagGeneration<File> implements ETagCache {
    //MD5映射表
    private final HashMap<File, ETagInfo> hashMap = new HashMap<>();
    private final Lock hashMapLock = new ReentrantLock();
    //MD5冲突检测表（MD5没有时，同时多次请求只算一次）
    private final Logger logger = Logger.getLogger(FileETagCache.class);

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private String hashCachePath = null;

    public void setProperties(FileProperties properties) {
        hashCachePath = properties.getEtagPath();
        if (!properties.isWatcher()) {
            if (properties.isCalcmd5()) {
                File localFile = new File(properties.getLocalDir());

                //在线程做，遍历缓存md5信息
                new Thread(() -> {
                    generateTree(localFile.toPath());
                    logger.info("All files generated.");
                }).start();
            }
        }
    }

    public void generateTree(Path path) {
//        loadFromFile(hashCachePath);
        hashMapLock.lock();
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    //排除点开头的
                    if (!dir.toString().contains("/.")) {
                        generateDir(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            hashMapLock.unlock();
//            saveToFile(hashCachePath);
        }
    }

    public String getETag(Path path) {
        File file = path.toFile();
        ETagInfo eTagInfo = getETagInfo(path);
        if (eTagInfo != null && eTagInfo.getLastModified() == file.lastModified() && eTagInfo.getSize() == file.length()) {
            return eTagInfo.getETag();
        }
        return generate(path.toFile(), new FileETagGenerationCallback<File>() {
            @Override
            public String generateETag(File file) {
                return getFileETag(file);
            }

            @Override
            public String afterGenerated(String ETag) {
                if (ETag == null) {
                    //等待其他地方生成完成的情况下，直接取最终的值
                    ETagInfo eTagInfo = getETagInfo(path);
                    if (eTagInfo != null) {
                        return eTagInfo.getETag();
                    }
                    logger.info("generate cause bug?");
                    return null;
                }
                return ETag;
            }
        });
    }

    //可从上传服务器直接设置结果
    public void setETag(Path path, String eTag) {
        logger.info("generated "+path+" ETag: " + eTag);
        File file = path.toFile();
        ETagInfo eTagInfo = new ETagInfo();
        eTagInfo.setETag(eTag);
        eTagInfo.setLastModified(file.lastModified());
        eTagInfo.setSize(file.length());
        putETag(file, eTagInfo);
//        saveToFile(hashCachePath);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void saveToFile(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    return;
                }
            }
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
            hashMapLock.lock();
            HashMap tempMap = new HashMap<>(hashMap);
            outputStream.writeObject(tempMap);
            hashMapLock.unlock();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({"unchecked", "UnusedDeclaration"})
    private void loadFromFile(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
                hashMapLock.lock();
                HashMap tempMap = (HashMap) inputStream.readObject();
                hashMap.putAll(tempMap);
                hashMapLock.unlock();
                inputStream.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private ETagInfo getETagInfo(Path path) {
        hashMapLock.lock();
        ETagInfo eTagInfo = hashMap.get(path.toFile());
        hashMapLock.unlock();
        return eTagInfo;
    }

    private void putETag(File path, ETagInfo eTagInfo) {
        hashMapLock.lock();
        hashMap.put(path, eTagInfo);
        hashMapLock.unlock();
    }

    public void removeETag(Path path) {
        hashMapLock.lock();
        hashMap.remove(path.toFile());
        hashMapLock.unlock();
//        saveToFile(hashCachePath);
    }

    private void generateDir(Path dir) {
        File dirFile = dir.toFile();
        if (!dirFile.isDirectory()) {
            return;
        }
        File[] files = dir.toFile().listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String filename = file.getName();
            if (!file.isFile() || filename.indexOf(".") == 0) {
                continue;
            }

            //带缓存的，当文件没有改变，无需重新计算ETag值（可能存在其他风险，服务器本身保证了文件的正确性会更可靠
            // 另外，离线移除没有判断，会有无用的数据堆积，后续考虑改进）
            getETag(file.toPath());
            //每次都计算MD5的情况，启动变慢，文件多了以后就会变得无限慢
//            generate(file, new FileETagGenerationCallback<File>() {
//                @Override
//                public String generateETag(File file) {
//                    return getFileETag(file);
//                }
//
//                @Override
//                public String afterGenerated(String ETag) {
//                    return null;
//                }
//            });
        }
    }

    private String getFileETag(File file) {
        String ETag = FileHash.getFileETag(file);
        setETag(file.toPath(), ETag);
        return ETag;
    }
}
