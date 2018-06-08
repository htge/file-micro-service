package com.htge.login.controller.Map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.htge.login.model.UserinfoDao;
import com.htge.login.model.Userinfo;
import com.htge.login.util.LoginManager;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Component
public class SettingMap {
	@Resource(name = "UserinfoDao")
	private UserinfoDao userinfoDao;

	public ModelAndView settingPage(HttpServletRequest request) {
		Subject subject = SecurityUtils.getSubject();
		if (LoginManager.isAuthorized(subject)) {
			String oldUserName = (String)subject.getSession().getAttribute(LoginManager.SESSION_USER_KEY);
			//这边后续加分页支持、排序、查找
			Collection<Userinfo> users = userinfoDao.getAllUser();
			List<HashMap <String, Object>> userList = new ArrayList<>();
			String currentUserName = null;
			String currentUserRole = null;
			//转换用户名表
			for (Userinfo objs : users) {
				String username = objs.getUsername();
				String role = "普通用户";

				//其他用户
				if (!username.equals("admin") && !username.equals(oldUserName)) {
					//只有管理员用户可以获取
					if (LoginManager.getUserRule(subject, userinfoDao) == LoginManager.LoginRole.Admin) {
						if (objs.getRole() == LoginManager.LoginRole.Admin) {
							role = "管理员";
						}
						HashMap<String, Object> map = new HashMap<>();
						map.put("username", username);
						map.put("role", role);
						userList.add(map);
					}
				} else if (username.equals(oldUserName)) { //当前账户
					currentUserName = username;
					if (objs.getRole() == LoginManager.LoginRole.Admin) {
						currentUserRole = "管理员";
					} else {
						currentUserRole = "普通用户";
					}
				}
			}

			ModelAndView view = new ModelAndView("setting");
			if (currentUserName != null) {
				view.addObject("username", currentUserName);
			}
			if (currentUserRole != null) {
				view.addObject("role", currentUserRole);
			}
			view.addObject("usersrcs", userList);
			view.addObject("Path", request.getSession().getServletContext().getContextPath());
			return view;
		}
		return new ModelAndView("redirect:/auth/");
	}
}
