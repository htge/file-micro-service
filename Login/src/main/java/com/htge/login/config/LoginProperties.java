package com.htge.login.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "login")
public class LoginProperties {
    private String rootPath = "/auth/"; //当前服务根路径
    private String nextRoot = "/"; //登录成功后跳转的路径
    private String uploadRoot = "/up/"; //上传服务的路径
    private int cacheTotal = 200000; //数据库缓存的总量，总量越大，耗时越长
    private int cacheUnit = 10000; //数据库一次缓存的数量，数值越大对内存消耗影响越大，超过一定值后，不会提高缓存速度
    private boolean cacheEnabled = false;
    private int sessionTimeout = 7200000; //Session超时

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setNextRoot(String nextRoot) {
        this.nextRoot = nextRoot;
    }

    public String getNextRoot() {
        return nextRoot;
    }

    public void setUploadRoot(String uploadRoot) {
        this.uploadRoot = uploadRoot;
    }

    public String getUploadRoot() {
        return uploadRoot;
    }

    public void setCacheTotal(int cacheTotal) {
        this.cacheTotal = cacheTotal;
    }

    public int getCacheTotal() {
        return cacheTotal;
    }

    public void setCacheUnit(int cacheUnit) {
        this.cacheUnit = cacheUnit;
    }

    public int getCacheUnit() {
        return cacheUnit;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }
}
