package com.htge.login.controller.Map;

import java.util.*;

import com.htge.login.config.LoginProperties;
import com.htge.login.model.UserinfoDao;
import com.htge.login.model.Userinfo;
import com.htge.login.util.LoginManager;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SettingMap {
	private UserinfoDao userinfoDao;
    private LoginProperties properties;

	public void setUserinfoDao(UserinfoDao userinfoDao) {
		this.userinfoDao = userinfoDao;
	}

	public void setProperties(LoginProperties properties) {
		this.properties = properties;
	}

	public ModelAndView settingPage(HttpServletRequest request, HttpServletResponse response) {
		Subject subject = SecurityUtils.getSubject();
		Session session = subject.getSession();
		if (LoginManager.isAuthorized(subject)) {
			String oldUserName = (String)session.getAttribute(LoginManager.SESSION_USER_KEY);

            //当前用户信息
            Userinfo userinfo = userinfoDao.findUser(oldUserName);

            ModelAndView view = new ModelAndView("setting");

			if (userinfo != null) {
				view.addObject("username", oldUserName);
				view.addObject("role", userinfo.getRole()==LoginManager.LoginRole.Admin?"管理员":"普通用户");
			}
			view.addObject("Path", request.getSession().getServletContext().getContextPath());
			return view;
		}
		return LoginManager.redirectToRoot(session, request, response);
	}
}
