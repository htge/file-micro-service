package com.htge.login.util;

import com.htge.login.model.UserinfoDao;
import com.htge.login.model.Userinfo;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.jboss.logging.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@ConfigurationProperties(prefix = "server.session.cookie")
public class LoginManager {
	public static final String SESSION_USER_KEY = "username";
	private static final Logger logger = Logger.getLogger(LoginManager.class);
	private static String sessionId = "JSESSIONID";

	@Resource(name = "simpleCookie")
	private SimpleCookie simpleCookie;

	public void setName(String name) {
		Assert.hasText(name, "server.session.cookie.name不能为空");
		LoginManager.sessionId = name;
		simpleCookie.setName(name);
	}

	@SuppressWarnings({"unused", "WeakerAccess"})
	public static class LoginRole {
		public static final int Error = -1;
		public static final int Normal = 0;
		public static final int Admin = 1;
	}

	//根据请求获取当前用户的角色：0=普通用户 1=管理员
	public static int getUserRule(Subject subject, UserinfoDao userItenDao) {
		if (isAuthorized(subject)) {
			String username = (String)subject.getSession().getAttribute(SESSION_USER_KEY);
			Userinfo userinfo = userItenDao.findUser(username);
			if (userinfo != null) {
				return userinfo.getRole();
			}
		}
		return LoginRole.Error;
	}

	public static void updateSessionInfo(Session session, String username, int timeout) {
		if (session == null || username == null) {
			return;
		}
		if (session.getAttribute(SESSION_USER_KEY) == null) { //防止模拟post请求后导致注销
			session.setTimeout(timeout); //登录后，设置为更长的过期时间
			session.setAttribute(SESSION_USER_KEY, username);
		}
	}

	public static boolean isAuthorized(Subject subject) {
		if (subject == null) {
			return false;
		}
		Session session = subject.getSession();
		if (session == null) {
			logger.error("isAuthorized() empty session found.");
			return false;
		}
		String username = (String)session.getAttribute(SESSION_USER_KEY);
		return (username != null);
	}

	public static void Logout(Subject subject) {
		Session session = subject.getSession();
		Object username = session.getAttribute(SESSION_USER_KEY);
		if (username != null) {
			session.removeAttribute(SESSION_USER_KEY);
		}
		subject.logout();
	}

	public static ModelAndView redirectToRoot(Session session, HttpServletRequest request, HttpServletResponse response) {
		final String HttpSetCookieKey = "Set-Cookie";
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
		try {
			response.sendRedirect("/auth/");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
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
}
