package com.htge.login.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("LoginProperties")
@ConfigurationProperties(prefix = "login")
public class LoginProperties {
    private String nextRoot = "/";
    private String rootPath = "/auth/";

    public void setNextRoot(String nextRoot) {
        this.nextRoot = nextRoot;
    }

    public String getNextRoot() {
        return nextRoot;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getRootPath() {
        return rootPath;
    }
}
