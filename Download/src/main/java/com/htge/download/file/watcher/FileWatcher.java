package com.htge.download.file.watcher;

import com.htge.download.config.FileProperties;
import com.htge.download.file.cache.ETagCache;
import com.htge.download.file.util.FileHash;
import com.sun.nio.file.SensitivityWatchEventModifier;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;

public class FileWatcher {
    private WatchService watchService = null;
    private final HashMap<WatchKey, Path> watchMap = new HashMap<>();
    private final Logger logger = Logger.getLogger(FileWatcher.class);

    private ETagCache eTagCache = null;

    public void seteTagCache(ETagCache eTagCache) {
        this.eTagCache = eTagCache;
    }

    @SuppressWarnings({"Convert2MethodRef", "CodeBlock2Expr"})
    public void setProperties(FileProperties properties) {
        if (properties.isWatcher()) {
            File localFile = new File(properties.getLocalDir());

            //在线程做，遍历缓存md5信息
            new Thread(()-> {
                watchPathTree(localFile.toPath());
            }).start();
        }
    }

    private void watchPath(Path path) throws IOException {
        WatchKey key = path.register(watchService,
                new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY},
                SensitivityWatchEventModifier.HIGH);
        watchMap.put(key, path);
    }

    /* 增量更新 */
    @SuppressWarnings({"Convert2MethodRef", "CodeBlock2Expr"})
    private void generateFileETagAsync(File file) {
        Thread thread = new Thread(() -> {
            generateFileETag(file);
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /* 一次性获取 */
    private void generateFileETag(File file) {
        Path newPath = file.toPath();
        String eTag = FileHash.getFileETag(file);
        logger.info("generated "+newPath+" MD5: " + eTag);
        eTagCache.setETag(newPath, eTag);
    }

    private void watchPathTree(Path path) {
        eTagCache.generateTree(path);
        new Thread(() -> {
            try {
                watchService = FileSystems.getDefault().newWatchService();
                while (true) {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }
                        Path filename = (Path) event.context();
                        String name = filename.toString();
                        if (name.indexOf(".") == 0) {
                            continue;
                        }

                        Path dir = watchMap.get(key);
                        Path newPath = dir.resolve(filename);
                        if (Files.isDirectory(newPath, LinkOption.NOFOLLOW_LINKS)) {
                            logger.info("dir = " + newPath + " event = " + kind);
                            if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                                watchPath(newPath);
                            }
                        } else {
                            logger.info("file = " + newPath + " event = " + kind);
                            if (event.kind() != StandardWatchEventKinds.ENTRY_DELETE) {
                                generateFileETagAsync(newPath.toFile());
                            } else {
                                eTagCache.removeETag(newPath);
                            }
                        }
                    }
                    boolean valid = key.reset();
                    if (!valid) {
                        watchMap.remove(key);
                        if (watchMap.isEmpty()) {
                            break;
                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
