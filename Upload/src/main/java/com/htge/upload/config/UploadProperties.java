package com.htge.upload.config;

import org.springframework.beans.InvalidPropertyException;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "upload")
public class UploadProperties {
    private String rootPath = "/Volumes/macfiles"; //路径结尾不带反斜杠
    private boolean authorization = false; //账号认证

    public void setRootPath(String rootPath) {
        if (rootPath.length() == 0) {
            throw new InvalidPropertyException(this.getClass(), "upload.rootPath", "Value could not be empty");
        }
        if (rootPath.substring(rootPath.length()-1).equals("/")) {
            throw new InvalidPropertyException(this.getClass(), "file.serverRoot", "Path invalid. Valid format: /xxx/xxx");
        }
        this.rootPath = rootPath;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setAuthorization(boolean authorization) {
        this.authorization = authorization;
    }

    public boolean isAuthorization() {
        return authorization;
    }
}
