package com.htge.download.rabbit;

import com.htge.download.file.cache.FileETagCache;
import net.sf.json.JSONObject;
import org.jboss.logging.Logger;

import java.io.File;

public class UploadData implements RPCData {
    private Logger logger = Logger.getLogger(UploadData.class);
    private FileETagCache fileETagCache = null;

    public void setFileETagCache(FileETagCache fileETagCache) {
        this.fileETagCache = fileETagCache;
    }

    @Override
    public String parseData(String data) {
        logger.info("RPC Request: "+data);
        JSONObject resultObject = new JSONObject();
        try {
            JSONObject jsonObject = JSONObject.fromObject(data);
            String path = jsonObject.getString("path");
            String eTag = jsonObject.getString("eTag");
            fileETagCache.setETag(new File(path).toPath(), eTag);

            resultObject.put("result", "success");
            return resultObject.toString();
        } catch (Exception e) {
            e.printStackTrace();
            resultObject.put("error", e.getMessage());
            return resultObject.toString();
        }
    }
}
