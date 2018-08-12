package com.htge.download.rabbit.login;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class LoginData {
    private boolean isValidSession;
    private ROLE role;
    private String rootPath;
    private String settingPath;
    private String errorMessage;

    @SuppressWarnings("unused")
    private static class LoginRole {
        static final int Error = -1;
        static final int Normal = 0;
        static final int Admin = 1;
    }

    public enum ROLE {
        Normal, Admin, Undefined
    }

    LoginData(String data) throws JSONException {
        JSONObject jsonObject = JSONObject.fromObject(data);
        if (jsonObject.containsKey("error")) {
            errorMessage = jsonObject.getString("error");
        } else {
            isValidSession = jsonObject.getBoolean("isValidSession");
            rootPath = jsonObject.getString("rootPath");
            switch (jsonObject.getInt("role")) {
                case LoginRole.Normal:
                    role = ROLE.Normal;
                    break;
                case LoginRole.Admin:
                    role = ROLE.Admin;
                    break;
                default:
                    role = ROLE.Undefined;
                    break;
            }
            settingPath = jsonObject.getString("settingPath");
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

    public ROLE getRole() {
        return role;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
