package com.htge.download.file.cache;

import com.htge.download.file.util.FileHash;
import org.jboss.logging.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ConcurrentHashMap;

@Component("FileETagCache")
public class FileETagCache implements ETagCache {
    private final ConcurrentHashMap<Path, ETagInfo> hashMap = new ConcurrentHashMap<>();
    private final Logger logger = Logger.getLogger(FileETagCache.class);

    public void generateTree(Path path) {
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
        }
    }

    public String getETag(Path path) {
        File file = path.toFile();
        ETagInfo eTagInfo = hashMap.get(path);
        if (eTagInfo != null && eTagInfo.getLastModified() == file.lastModified() && eTagInfo.getSize() == file.length()) {
            return eTagInfo.getETag();
        }
        return generateETag(path.toFile());
    }

    //可从上传服务器直接设置结果
    public void setETag(Path path, String eTag) {
        logger.info("generated "+path+" ETag: " + eTag);
        File file = path.toFile();
        ETagInfo eTagInfo = new ETagInfo();
        eTagInfo.setETag(eTag);
        eTagInfo.setLastModified(file.lastModified());
        eTagInfo.setSize(file.length());
        hashMap.put(file.toPath(), eTagInfo);
    }

    public void removeETag(Path path) {
        hashMap.remove(path);
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
            generateETag(file);
        }
    }

    private String generateETag(File file) {
        String ETag = FileHash.getFileMD5(file);
        setETag(file.toPath(), ETag);
        return ETag;
    }

    private class ETagInfo {
        private String ETag;
        private long LastModified;
        private long size;

        private void setETag(String ETag) {
            this.ETag = ETag;
        }

        private String getETag() {
            return ETag;
        }

        private void setLastModified(long lastModified) {
            LastModified = lastModified;
        }

        private long getLastModified() {
            return LastModified;
        }

        private void setSize(long size) {
            this.size = size;
        }

        private long getSize() {
            return size;
        }
    }
}
