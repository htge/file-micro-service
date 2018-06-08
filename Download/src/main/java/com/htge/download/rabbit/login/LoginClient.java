package com.htge.download.rabbit.login;

import com.htge.download.rabbit.RPCClient;
import net.sf.json.JSONObject;
import org.jboss.logging.Logger;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;

import java.io.IOException;

public class LoginClient {
    private RPCClient fibonacciRpc = null;
    private Logger logger = Logger.getLogger(LoginClient.class);

    public LoginClient(CachingConnectionFactory factory) {
        try {
            fibonacciRpc = new RPCClient(factory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LoginData getLoginInfo(String sessionId) {
        final String requestQueueName = "login_queue";
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sessionId", sessionId);
            String response = fibonacciRpc.call(requestQueueName, jsonObject.toString());
            logger.info("getLoginInfo() repsonse = " + response);
            return new LoginData(response);
        }
        catch  (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void finalize() {
        if (fibonacciRpc!= null) {
            fibonacciRpc.close();
        }
    }
}
