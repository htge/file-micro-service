package com.htge.download.boot;

import com.htge.download.config.FileProperties;
import com.htge.download.file.FileMap;
import com.htge.download.file.cache.FileETagCache;
import com.htge.download.file.watcher.FileWatcher;
import com.htge.download.rabbit.RecvUploadServer;
import com.htge.download.rabbit.login.LoginClient;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

@Component
public class ApplicationStartup implements ApplicationListener<ContextRefreshedEvent> {
    @Resource(name = "rabbitConnectionFactory")
    private CachingConnectionFactory factory;
    @Resource(name = "FileMap")
    private FileMap fileMap;
    @Resource(name = "FileProperties")
    private FileProperties fileProperties;
    @Resource(name = "FileETagCache")
    private FileETagCache fileETagCache;
    @Resource(name = "FileWatcher")
    private FileWatcher fileWatcher;

    private static boolean isDone = false;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (!isDone) {
            isDone = true;

            RecvUploadServer.createInstance(factory);

            //fileMap
            File localFile = new File(fileProperties.getLocalDir());
            if (fileProperties.isWatcher()) {
                fileWatcher.seteTagCache(fileETagCache);
                fileWatcher.watchPathTree(localFile.toPath());
            } else {
                fileETagCache.generateTree(localFile.toPath());
            }
            fileMap.setLoginClient(new LoginClient(factory));
            fileMap.seteTagCache(fileETagCache);
            fileMap.setProperties(fileProperties);
        }
    }
}
