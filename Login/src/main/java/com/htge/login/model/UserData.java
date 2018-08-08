package com.htge.login.model;

import com.htge.login.util.Crypto;
import net.sf.json.JSONObject;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.util.Arrays;

/*登录，注册使用的数据结构*/
public class UserData {
	private String username;
	private String password;
	private String validation;
	private String userdata;
	private String role;
	private String encryptedData;
	private String encryptedKey;
	private String newPassword;
	private long timestamp;
	private String uuid;

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		return this.password;
	}

	public String getValidation() {
		return this.validation;
	}

	@SuppressWarnings("unused")
	public String getUserData() {
		return this.userdata;
	}

	public String getRole() {
		return role;
	}

	public String getNewPassword() { return newPassword; }

	public long getTimestamp() {
		return timestamp;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@SuppressWarnings("unused")
	public void setValidation(String validation) {
		this.validation = validation;
	}

	public void setUserData(String userData) {
		this.userdata = userData;
	}

	@SuppressWarnings("unused")
	public void setRole(String role) {
		this.role = role;
	}

	public void setData(String data) {
		encryptedData = data;
	}

	public void setKey(String key) {
		encryptedKey = key;
	}

	public void decryptDatas(PrivateKey privateKey) {
		try {
			byte[] decrypted = Crypto.decryptRSA(encryptedKey, privateKey);
			if (decrypted != null && decrypted.length > 0) {
				String decryptKey = new String(decrypted, "UTF-8");
				String loginInfo = Crypto.decryptFromPage(encryptedData, decryptKey);
				JSONObject jsonObject = JSONObject.fromObject(loginInfo);
				if (jsonObject.containsKey("username")) {
					username = jsonObject.getString("username");
				}
				password = jsonObject.getString("password");
				if (jsonObject.containsKey("newPassword")) {
					newPassword = jsonObject.getString("newPassword");
				}
				if (jsonObject.containsKey("validation")) {
					validation = jsonObject.getString("validation");
				}
				if (jsonObject.containsKey("role")) {
					role = jsonObject.getString("role");
				}
				timestamp = jsonObject.getLong("timestamp");
				uuid = jsonObject.getString("uuid");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
