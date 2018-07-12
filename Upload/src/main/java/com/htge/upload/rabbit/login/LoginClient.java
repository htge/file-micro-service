package com.htge.upload.rabbit.login;

import com.htge.upload.rabbit.RPCClient;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.jboss.logging.Logger;

public class LoginClient {
    private RPCClient rpcClient = null;
    private Logger logger = Logger.getLogger(LoginClient.class);

    public void setRpcClient(RPCClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public LoginData getLoginInfo(String sessionId) {
        final String requestQueueName = "login_queue";
        try {
            JSONObject jsonObject = new JSONObject();
            if (sessionId != null) {
                jsonObject.put("sessionId", sessionId);
            }
            String response = rpcClient.call(requestQueueName, jsonObject.toString());
            logger.info("getLoginInfo() repsonse = " + response);
            return new LoginData(response);
        }
        catch (JSONException e) {
            logger.warn("LoginData incorrect");
//            e.printStackTrace();
        }
        return null;
    }
}
