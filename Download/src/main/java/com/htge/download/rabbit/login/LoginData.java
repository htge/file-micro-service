package com.htge.download.rabbit.login;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class LoginData {
    private boolean isValidSession;
    private int role;
    private String rootPath;
    private String settingPath;
    private String logoutPath;
    private String errorMessage;

    @SuppressWarnings("unused")
    public static class LoginRole {
        public static final int Error = -1;
        public static final int Normal = 0;
        public static final int Admin = 1;
    }

    LoginData(String data) throws JSONException {
        JSONObject jsonObject = JSONObject.fromObject(data);
        if (jsonObject.containsKey("error")) {
            errorMessage = jsonObject.getString("error");
        } else {
            isValidSession = jsonObject.getBoolean("isValidSession");
            rootPath = jsonObject.getString("rootPath");
            role = jsonObject.getInt("role");
            if (role == LoginRole.Admin) {
                settingPath = jsonObject.getString("settingPath");
            }
            logoutPath = jsonObject.getString("logoutPath");
        }
    }

    public boolean isValidSession() {
        return isValidSession;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getSettingPath() {
        return settingPath;
    }

    public int getRole() {
        return role;
    }

    public String getLogoutPath() {
        return logoutPath;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
