package com.htge.upload.controller.util;

import com.htge.upload.config.UploadProperties;
import com.htge.upload.rabbit.login.LoginClient;
import com.htge.upload.rabbit.login.LoginData;
import net.sf.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class UploadUtil {
    private LoginClient loginClient;
    private UploadProperties uploadProperties;

    public void setLoginClient(LoginClient loginClient) {
        this.loginClient = loginClient;
    }
    public void setUploadProperties(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    public enum LoginStatus {
        RPC_Failed,
        NOT_Logined,
        LOGIN_Denied,
        Success,
    }

    public Object getLoginStatus(HttpServletRequest request, GetLoginStatusCallback callback) throws Exception {
        if (uploadProperties.isAuthorization()) {
            String contextPath = URLDecoder.decode(request.getRequestURI(), "UTF-8");
            String sessionId = request.getRequestedSessionId();
            if (sessionId == null) {
                sessionId = request.getSession().getId();
            }
            String requestPath = null;
            if (request.getMethod().equals("GET")) {
                int port = request.getServerPort();
                if (port != 80 && port != 443) {
                    requestPath = String.format("//%s:%s%s", request.getServerName(), request.getServerPort(), contextPath);
                } else {
                    requestPath = String.format("//%s%s", request.getServerName(), contextPath);
                }
            }
            LoginData loginData = loginClient.getLoginInfo(sessionId, requestPath);
            if (loginData == null) {
                return callback.execute(LoginStatus.RPC_Failed, null);
            }
            if (loginData.getErrorMessage() != null || !loginData.isValidSession()) {
                return callback.execute(LoginStatus.NOT_Logined, loginData);
            }
            if (loginData.getRole() != LoginData.ROLE.Admin) {
                return callback.execute(LoginStatus.LOGIN_Denied, loginData);
            }
        }
        return callback.execute(LoginStatus.Success, null);
    }

    public interface GetLoginStatusCallback {
        Object execute(UploadUtil.LoginStatus loginStatus, LoginData loginData) throws Exception;
    }

    @SuppressWarnings({"Convert2MethodRef", "CodeBlock2Expr"})
    public void buildFileList(File dir, String pid, ArrayList<JSONObject> arrayList) {
        File[] dirs = dir.listFiles((File pathname) -> {
            return pathname.isDirectory();
        });
        if (dirs != null) {
            for (File subDir : dirs) {
                //节点
                String name = subDir.getName();
                String id = pid+name+"/";
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", id);
                jsonObject.put("pId", pid);
                jsonObject.put("name", name);
                arrayList.add(jsonObject);

                buildFileList(subDir, id, arrayList);
            }
        }
    }

    @SuppressWarnings({"Convert2MethodRef", "CodeBlock2Expr", "unused", "WeakerAccess"})
    public Map<String, Object> buildFileTree(File dir) {
        File[] dirs = dir.listFiles((File pathname) -> {
            return pathname.isDirectory();
        });
        Map<String, Object> tree = null;
        if (dirs != null) {
            tree = new TreeMap<>();
            for (File subDir : dirs) {
                Map<String, Object> subTree = buildFileTree(subDir);
                tree.put(subDir.getName(), subTree);
            }
        }
        return tree;
    }
}
