package com.htge.download.file.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

public class FileHash {
    public static String getFileMD5(File file) {
        InputStream inputStream = null;
        String MD5 = null;
        try {
            int len;
            byte[] buffer = new byte[65536];
            inputStream = new FileInputStream(file);
            MessageDigest md5 = MessageDigest.getInstance("md5");
            while ((len = inputStream.read(buffer)) != -1) {
                md5.update(buffer, 0, len);
            }
            byte[] result = md5.digest();
            MD5 = new BigInteger(1, result).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "\""+MD5+"\"";
    }
}
