package com.htge.login.controller.Map;

import java.security.KeyPair;
import java.security.PrivateKey;

import com.htge.login.model.UserData;
import com.htge.login.model.UserinfoDao;
import com.htge.login.model.Userinfo;
import com.htge.login.util.Crypto;
import com.htge.login.util.LoginManager;
import com.htge.login.util.ResponseGeneration;
import net.sf.json.JSONObject;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RegisterMap {
	private UserinfoDao userinfoDao;

	public void setUserinfoDao(UserinfoDao userinfoDao) {
		this.userinfoDao = userinfoDao;
	}

	public Object registerPage(HttpServletRequest request, HttpServletResponse response) {
		Subject subject = SecurityUtils.getSubject();
		Session session = subject.getSession();
		if (LoginManager.getUserRule(subject, userinfoDao) == LoginManager.LoginRole.Admin) {
			if (session == null) {
				return new ResponseEntity<>("{}", HttpStatus.INTERNAL_SERVER_ERROR);
			}
			KeyPair keyPair = (KeyPair) session.getAttribute("keypair");
			if (keyPair == null) {
				keyPair = Crypto.getCachedKeyPair();
			}
			if (keyPair != null) {
				String rsaPublicKey = Crypto.getPublicKey(keyPair);
				session.setAttribute("keypair", keyPair);
				ModelAndView view = new ModelAndView("register", "rsa", rsaPublicKey);
				view.addObject("Path", request.getSession().getServletContext().getContextPath());
				return view;
			}
		}
		return LoginManager.redirectToRoot(session, request, response);
	}

	public ResponseEntity register(@ModelAttribute UserData userData) {
		Subject subject = SecurityUtils.getSubject();
		Session session = subject.getSession();
		String username = "";
		if (session != null) {
			KeyPair keyPair = (KeyPair) session.getAttribute("keypair");
			if (keyPair != null) {
				PrivateKey privateKey = keyPair.getPrivate();
				userData.decryptDatas(privateKey);
				username = userData.getUsername();
				String password = userData.getPassword();
				String validation = userData.getValidation();
				if (!username.matches("^[a-zA-Z0-9]+$")) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("message", "用户名只能允许字母和数字");
					return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
				}
				if (username.length() < 4 || username.length() > 20) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("message", "用户名长度必须在4～20之间");
					return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
				}
				if (password == null || password.length() < 8 || password.length() > 32) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("message", "密码长度必须在8～32之间");
					return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
				}
				if (validation == null || !password.equals(validation)) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("message", "密码输入不匹配");
					return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
				}
				if (LoginManager.getUserRule(subject, userinfoDao) == LoginManager.LoginRole.Admin) {
					//加密用户信息
					String userDataStr = Crypto.generateUserData(username, password);
					userData.setUserData(userDataStr);
					Userinfo userinfo = userinfoDao.findUser(username);
					if (userinfo != null) {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("message", "用户'"+username+"'已存在");
						return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
					}
					userinfo = new Userinfo();
					userinfo.setUsername(username);
					userinfo.setUserdata(userDataStr);
					String role = userData.getRole();
					if (role != null && role.equals("admin")) {
						userinfo.setRole(1);
					} else {
						userinfo.setRole(0);
					}
					if (!userinfoDao.create(userinfo)) {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("message", "注册新用户'"+username+"'失败");
						return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
					}
					session.removeAttribute("keypair");
					return new ResponseEntity<>("{}", HttpStatus.OK);
				}
			}
		}
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("message", "注册新用户'"+username+"'失败");
		return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
	}
}
