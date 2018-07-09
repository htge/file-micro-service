package com.htge.login.util;

import java.security.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {
	/*############### SHA384 ##############*/
	private static final String asciiTable = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ`~!@#$%^&*()_+-={}|[]\\:\";'<>?,./'";
	private static String getStringFromByte(byte []bytes) {
		StringBuilder sb = new StringBuilder();
		for(byte b : bytes) {
			sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}

	private static String getSHA384(String str, String salt) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-384");
			md.update(salt.getBytes("UTF-8"));
			byte[] bytes = md.digest(str.getBytes("UTF-8"));
			return getStringFromByte(bytes);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	public static String generateUserData(String username, String password) {
		if (username == null || password == null || username.length() == 0 || password.length() == 0) {
			return "";
		}
		byte salt[] = new byte[128];
		String concat = username+password;
		for (int i=0; i<salt.length; i++) { //第一遍，填充盐
			int idx0 = (i+64)*22;
			int idx = (idx0+concat.length()+concat.charAt(idx0%concat.length()))%asciiTable.length();
			salt[i] = (byte)asciiTable.charAt(idx);
		}

		String saltString = new String(salt);
		return getSHA384(concat, saltString); //第二遍，获取sha384值
	}

	/*############### Base64 ##############*/
	private static byte[] Base64Decript(String base64Str) {
		try {
			return Base64.decodeBase64(base64Str);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/*############### AES ##############*/
	private static String AESDecrypt(byte[] bytes, String password) {
		try {
			SecretKey key = new SecretKeySpec(password.getBytes("UTF-8"), "AES");
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] contents = cipher.doFinal(bytes);
			return new String(contents,"utf-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String decryptFromPage(String base64Str, String password) {
		byte[] cryptedData = Base64Decript(base64Str);
		if (cryptedData != null) {
			return AESDecrypt(cryptedData, password);
		}
		return null;
	}

	/*############### RSA ##############*/
	static private final Provider provider = new BouncyCastleProvider();
	static private KeyPair cachedKeyPair = generateRSAKeyPairs();
	static private final Lock lock = new ReentrantLock();
	static private final int KEYPAIR_TIMEOUT = 300000; //因为RSA信息产生的时间长，5分钟换一次
	static private final int KEY_SIZE = 3072;

	static {
		//永久的计时器，定时更换RSA密钥对
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				KeyPair newCachedKeyPair = generateRSAKeyPairs();
				lock.lock();
				cachedKeyPair = newCachedKeyPair;
				lock.unlock();
			}
		}, KEYPAIR_TIMEOUT, KEYPAIR_TIMEOUT);
	}

	private static KeyPair generateRSAKeyPairs() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", provider);
			generator.initialize(KEY_SIZE);
			return generator.generateKeyPair();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static KeyPair getCachedKeyPair() {
		lock.lock();
		KeyPair keyPair = cachedKeyPair;
		lock.unlock();
		return keyPair;
	}

	public static String getPublicKey(KeyPair keyPair) {
		if (keyPair != null) {
			BCRSAPublicKey publicKey = (BCRSAPublicKey) keyPair.getPublic();
			return publicKey.getModulus().toString(16);
		}
		return null;
	}

	public static byte[] decryptRSA(byte[] encrypted, PrivateKey privateKey) {
		try { //约定好客户端只会传小于等于128字节的数据，否则就不解密
			Cipher cipher = Cipher.getInstance("RSA", provider);
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			byte[] ret = cipher.doFinal(encrypted);
			if (ret != null) { //拿到实际数据
				int index = 0, length = ret.length;
				for (int i=ret.length; i>0; i--) {
					byte b = ret[i-1];
					if (b == 0) { //字符串结尾
						index = i;
						length = ret.length-i;
						break;
					}
				}
				byte[] act = new byte[length];
				System.arraycopy(ret, index, act, 0, length);
				return act;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
