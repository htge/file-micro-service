package com.htge.upload.rabbit.upload;

import com.htge.upload.rabbit.RPCClient;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.jboss.logging.Logger;

public class UploadClient {
    private RPCClient rpcClient = null;
    private Logger logger = Logger.getLogger(UploadClient.class);

    public void setRpcClient(RPCClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public boolean setUploadFileETag(String path, String eTag) {
        final String requestQueueName = "upload_queue";
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("path", path);
            jsonObject.put("eTag", eTag);

            String response = rpcClient.call(requestQueueName, jsonObject.toString());
            logger.info("getLoginInfo() repsonse = " + response);
            JSONObject resultObject = JSONObject.fromObject(response);
            return !resultObject.has("error");
        }
        catch (JSONException e) {
            logger.warn("LoginData incorrect");
//            e.printStackTrace();
        }
        return false;
    }
}
