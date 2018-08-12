package com.htge.login.util;

import com.htge.login.model.UserinfoDao;
import com.htge.login.model.Userinfo;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.jboss.logging.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyPair;
import java.util.Date;
import java.util.UUID;

@ConfigurationProperties(prefix = "server.session.cookie")
public class LoginManager {
	private static final Logger logger = Logger.getLogger(LoginManager.class);

	public class SESSION {
		public static final String KEYPAIR_KEY = "keypair";
		public static final String USER_KEY = "username";
		public static final String URL_KEY = "url";
		public static final String UUID_KEY = "uuid";
	}

	@SuppressWarnings({"unused", "WeakerAccess"})
	public static class LoginRole {
		public static final int Error = -1;
		public static final int Normal = 0;
		public static final int Admin = 1;
	}

	public enum ROLE {
		Normal, Admin, Undefined
	}

	//通过配置文件获取的变量
	private static String sessionId = "JSESSIONID";

	private SimpleCookie simpleCookie;
	private UserinfoDao userinfoDao;

	public void setSimpleCookie(SimpleCookie simpleCookie) {
		this.simpleCookie = simpleCookie;
	}
	public void setUserinfoDao(UserinfoDao userinfoDao) {
		this.userinfoDao = userinfoDao;
	}

	// Spring cloud配置的属性
	public void setName(String name) {
		Assert.hasText(name, "server.session.cookie.name不能为空");
		LoginManager.sessionId = name;
		simpleCookie.setName(name);
	}

	public boolean isValidSession(HttpServletRequest httpServletRequest) {
		Session session = SecurityUtils.getSubject().getSession();
		String sessionId = httpServletRequest.getRequestedSessionId();
		return (sessionId == null || session.getId().equals(sessionId));
	}

	public ROLE role() {
		try {
			String username = (String) SecurityUtils.getSubject().getSession().getAttribute(SESSION.USER_KEY);
			if (username != null) {
				Userinfo userinfo = userinfoDao.findUser(username);
				if (userinfo != null) {
					switch (userinfo.getRole()) {
						case LoginRole.Normal:
							return ROLE.Normal;
						case LoginRole.Admin:
							return ROLE.Admin;
						default:
							break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ROLE.Undefined;
	}

	public KeyPair generateKeyPair() throws Exception {
		Session session = SecurityUtils.getSubject().getSession();
		KeyPair keyPair = (KeyPair) session.getAttribute(SESSION.KEYPAIR_KEY);
		if (keyPair == null) {
			keyPair = Crypto.getCachedKeyPair();
		}
		if (keyPair == null) {
			throw new Exception("不能生成RSA信息");
		}
		session.setAttribute(SESSION.KEYPAIR_KEY, keyPair);
		return keyPair;
	}

	public String generateUUID() {
		Session session = SecurityUtils.getSubject().getSession();
		String uuid = (String)session.getAttribute(SESSION.UUID_KEY);
		if (uuid == null) {
			uuid = UUID.randomUUID().toString();
		}
		session.setAttribute(SESSION.UUID_KEY, uuid);
		return uuid;
	}

	public void updateSessionInfo(String username, int timeout) {
		Session session = SecurityUtils.getSubject().getSession();
		if (session == null || username == null) {
			return;
		}
		if (session.getAttribute(SESSION.USER_KEY) == null) { //防止模拟post请求后导致注销
			session.setTimeout(timeout); //登录后，设置为更长的过期时间
			session.setAttribute(SESSION.USER_KEY, username);
		}
	}

	public void Logout() {
		Subject subject = SecurityUtils.getSubject();
		Session session = subject.getSession();
		Object username = session.getAttribute(SESSION.USER_KEY);
		if (username != null) {
			session.removeAttribute(SESSION.USER_KEY);
		}
		subject.logout();
	}

	public void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final String HttpSetCookieKey = "Set-Cookie";
		Session session = SecurityUtils.getSubject().getSession();
		String newSessionId = session.getId().toString();
		String cookieInfo = response.getHeader(HttpSetCookieKey);

		//Shiro没有生成Cookie，执行以下代码生成，不过并不能解决sessionIdCookieEnabled设置为false的问题
		if (cookieInfo == null) {
			Cookie cookie = new Cookie(sessionId, newSessionId);
			cookie.setPath("/");
			cookie.setHttpOnly(true);
			response.addCookie(cookie);
			logger.info("Add Set-Cookie: "+response.getHeader(HttpSetCookieKey));
		} else {
			logger.info("Shiro generated Set-Cookie: "+cookieInfo);
		}
		//处理Cookie污染的问题导致Shiro无法正常找到指定的Session
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(sessionId)) {
					String oldSession = cookie.getValue();
					if (!oldSession.equals(newSessionId)) {
						Cookie newCookie = new Cookie(sessionId, oldSession);
						newCookie.setMaxAge(0);
						response.addCookie(newCookie);
						logger.info("Remove cookie: " + oldSession);
					}
				}
			}
		}
		String requestPath = request.getServletPath();
		if (!requestPath.equals("/auth/logout") && !requestPath.equals("/auth/")) {
			session.setAttribute(SESSION.URL_KEY, requestPath);
		}
		response.sendRedirect("/auth/");
	}

	/**
	 * 获取用户真实IP地址，不使用request.getRemoteAddr();的原因是有可能用户使用了代理软件方式避免真实IP地址,
	 *
	 * 可是，如果通过了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP值，究竟哪个才是真正的用户端的真实IP呢？
	 * 答案是取X-Forwarded-For中第一个非unknown的有效IP字符串。
	 *
	 * 如：X-Forwarded-For：192.168.1.110, 192.168.1.120, 192.168.1.130,
	 * 192.168.1.100
	 *
	 * 用户真实IP为： 192.168.1.110
	 */
	public static String getIpAddress(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_CLIENT_IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}

	public boolean isInvalidTimestamp(long timestamp) {
		//客户端时间与服务器时间在-2.5min~+2.5min之间
		long time = new Date().getTime() - timestamp;
		return (time >= 250000L || time <= -250000L);
	}
}
