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
			byte[] bytes = new BigInteger(encryptedKey, 16).toByteArray();
			if (bytes.length % 2 == 1 && bytes[0] == 0) { //转换的时候多转了一位
				bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
			}
			byte[] decrypted = Crypto.decryptRSA(bytes, privateKey);
			if (decrypted != null) {
				String decryptKey = new String(decrypted, "UTF-8");
				String loginInfo = Crypto.decryptFromPage(encryptedData, decryptKey);
				JSONObject jsonObject = JSONObject.fromObject(loginInfo);
				username = (String) jsonObject.get("username");
				password = (String) jsonObject.get("password");
				if (jsonObject.containsKey("newPassword")) { //修改密码的信息
					newPassword = (String) jsonObject.get("newPassword");
					validation = (String) jsonObject.get("validation");
				} else if (jsonObject.containsKey("validation")) { //注册用到的信息
					validation = (String) jsonObject.get("validation");
					role = (String) jsonObject.get("role");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
