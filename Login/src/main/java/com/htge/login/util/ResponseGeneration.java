package com.htge.login.util;

import net.sf.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseGeneration {
    public static ResponseEntity ResponseEntityWithJsonObject(JSONObject jsonObject, HttpStatus status) {
        byte[] bytes = convertJsonToUTF8(jsonObject);
        if (bytes != null) {
            return new ResponseEntity<>(bytes, status);
        }
        return new ResponseEntity<>(status);
    }

    private static byte[] convertJsonToUTF8(JSONObject object) {
        try {
            return object.toString().getBytes("UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
