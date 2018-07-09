package com.htge.download.config;

import org.springframework.beans.InvalidPropertyException;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "file")
public class FileProperties {
    private String localDir = "/"; //本地文件夹
    private String serverRoot = "/"; //相对路径
    private boolean authorization = false; //账号认证
    private boolean watcher = true; //检测文件变更
    private boolean calcmd5 = false; //启动的时候会自动计算local-dir下，所有文件的MD5，耗时长
    private String etagPath = "D:/ETags"; //ETag如果通过Hash算法生成，可以允许缓存到本地以加快ETag计算速度

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

    public void setCalcmd5(boolean calcmd5) {
        this.calcmd5 = calcmd5;
    }

    public boolean isCalcmd5() {
        return calcmd5;
    }

    public void setEtagPath(String etagPath) {
        this.etagPath = etagPath;
    }

    public String getEtagPath() {
        return etagPath;
    }
}
