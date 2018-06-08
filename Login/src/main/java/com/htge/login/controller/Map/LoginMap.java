package com.htge.login.controller.Map;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyPair;
import java.security.PrivateKey;

import com.htge.login.config.LoginProperties;
import com.htge.login.model.UserData;
import com.htge.login.util.Crypto;
import com.htge.login.util.LoginManager;
import com.htge.login.util.ResponseGeneration;
import net.sf.json.JSONObject;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Component
public class LoginMap {
	@Resource(name = "LoginProperties")
    private LoginProperties config;

	static {
		Crypto.getCachedKeyPair();
	}

	public Object loginPage(HttpServletRequest request) {
		Subject subject = SecurityUtils.getSubject();
		if (!LoginManager.isAuthorized(subject)) {
			String requestURI = request.getRequestURI();
			//http(s)://host/auth，重定向加一个反斜杠
			if (requestURI.lastIndexOf("/") != requestURI.length()-1) {
				return new ModelAndView("redirect:"+requestURI+"/");
			}
			try {
				Session session = subject.getSession();
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
					ModelAndView view = new ModelAndView("login", "rsa", rsaPublicKey);
					view.addObject("Path", request.getSession().getServletContext().getContextPath());
					return view;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new ModelAndView("redirect:"+config.getNextRoot());
	}

	public ResponseEntity login(@ModelAttribute UserData userData)
			throws IOException {
		Subject subject = SecurityUtils.getSubject();
		Session session = subject.getSession();
		if (session != null) {
			KeyPair keyPair = (KeyPair) session.getAttribute("keypair");
			if (keyPair != null) {
				PrivateKey privateKey = keyPair.getPrivate();
				userData.decryptDatas(privateKey);
				String username = userData.getUsername();

				if (LoginManager.isAuthorized(subject)) {
					return new ResponseEntity<>("{}", HttpStatus.BAD_REQUEST);
				}
				//加密用户信息
				UsernamePasswordToken token = new UsernamePasswordToken(userData.getUsername(), userData.getPassword());
				try {
					subject.login(token);

					//登录成功后，执行以下操作
					LoginManager.updateSessionInfo(subject.getSession(), username);
					session.removeAttribute("keypair");//清理之前的旧信息

					String lastUri = (String) session.getAttribute("url");
					StringBuilder url = new StringBuilder("");
					if (lastUri == null) {
						return new ResponseEntity<>("{}", HttpStatus.OK);
					}
					//解决乱码问题
					boolean isPath = (lastUri.lastIndexOf("/") == lastUri.length() - 1);
					String[] strings = lastUri.split("/");
					for (String str : strings) {
						if (str.length() != 0) {
							url.append("/");
							url.append(URLEncoder.encode(str, "UTF-8"));
						}
					}
					if (isPath) {
						url.append("/");
					}
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("url", url.toString());
					return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.OK);
				} catch (UnknownAccountException | IncorrectCredentialsException ex) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("message", "用户名或密码不正确，请重新输入");
					return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
				} catch (ExcessiveAttemptsException e) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("message", "输入密码错误次数过多，请过一会再试");
					return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
				}
			}
		}
		return ResponseGeneration.ResponseEntityWithJsonObject(new JSONObject(), HttpStatus.BAD_REQUEST);
	}
}