package com.htge.download.config;

import org.springframework.beans.InvalidPropertyException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("FileProperties")
@ConfigurationProperties(prefix = "file")
public class FileProperties {
    private String localDir = "/";
    private String serverRoot = "/";
    private boolean authorization = false;
    private boolean watcher = true;

    public void setLocalDir(String localDir) {
        if (localDir.length() == 0) {
            throw new InvalidPropertyException(this.getClass(), "file.localDir", "Value could not be empty");
        }
        if (localDir.substring(localDir.length()-1).equals("/")) {
            throw new InvalidPropertyException(this.getClass(), "file.localDir", "Path invalid. Valid format: /xxx/xxx");
        }
        this.localDir = localDir;
    }

    public String getLocalDir() {
        return localDir;
    }

    public void setServerRoot(String serverRoot) {
        if (serverRoot.length() < 4 || !serverRoot.substring(0, 4).equals("/dl/")) {
            throw new InvalidPropertyException(this.getClass(), "file.serverRoot", "Path invalid. Valid format: /dl/xxx");
        }
        if (!serverRoot.substring(serverRoot.length()-1).equals("/")) {
            throw new InvalidPropertyException(this.getClass(), "file.serverRoot", "Path invalid. Valid format: /xxx/xxx/");
        }
        this.serverRoot = serverRoot;
    }

    public String getServerRoot() {
        return serverRoot;
    }

    public void setAuthorization(boolean authorization) {
        this.authorization = authorization;
    }

    public boolean isAuthorization() {
        return authorization;
    }

    public void setWatcher(boolean watcher) {
        this.watcher = watcher;
    }

    public boolean isWatcher() {
        return watcher;
    }
}
