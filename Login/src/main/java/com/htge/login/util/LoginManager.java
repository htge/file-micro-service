package com.htge.login.util;

import com.htge.login.model.UserinfoDao;
import com.htge.login.model.Userinfo;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.jboss.logging.Logger;

public class LoginManager {
	public static final String SESSION_USER_KEY = "username";
	private static final Logger logger = Logger.getLogger(LoginManager.class);

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

	public static void updateSessionInfo(Session session, String username) {
		if (session == null || username == null) {
			return;
		}
		if (session.getAttribute(SESSION_USER_KEY) == null) { //防止模拟post请求后导致注销
			session.setTimeout(7200000); //登录后，设置为2小时过期
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
}
