package com.htge.download.file.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileHash {
    public static String getFileETag(File file) {
//        String md5 = getFileMD5(file);
//        if (md5 != null) {
        return String.format("\"%x-%x\"", file.lastModified(), file.length());
//        }
//        return null;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private static String getFileMD5(File file) {
        InputStream inputStream = null;
        String MD5 = null;
        try {
            inputStream = new FileInputStream(file);
            MD5 = getMD5(inputStream);
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
        return MD5;
    }

    private static String getMD5(InputStream inputStream) {
        String MD5 = null;
        try {
            int len;
            final byte[] buffer = new byte[1048576];
            final MessageDigest md5 = MessageDigest.getInstance("md5");
            while ((len = inputStream.read(buffer)) != -1) {
                md5.update(buffer, 0, len);
            }
            final BigInteger integer = new BigInteger(1, md5.digest());
            MD5 = String.format("%032x", integer);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return MD5;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private static String getSHA1(InputStream inputStream) {
        String MD5 = null;
        try {
            int len;
            final byte[] buffer = new byte[1048576];
            final MessageDigest md5 = MessageDigest.getInstance("SHA-1");
            while ((len = inputStream.read(buffer)) != -1) {
                md5.update(buffer, 0, len);
            }
            final BigInteger integer = new BigInteger(1, md5.digest());
            MD5 = String.format("%032x", integer);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return MD5;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private static String getSHA256(InputStream inputStream) {
        String MD5 = null;
        try {
            int len;
            final byte[] buffer = new byte[1048576];
            final MessageDigest md5 = MessageDigest.getInstance("SHA-256");
            while ((len = inputStream.read(buffer)) != -1) {
                md5.update(buffer, 0, len);
            }
            final BigInteger integer = new BigInteger(1, md5.digest());
            MD5 = String.format("%032x", integer);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return MD5;
    }
}
