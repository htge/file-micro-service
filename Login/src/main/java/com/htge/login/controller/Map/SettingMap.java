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

			//这边后续加分页支持、排序、查找
			final int pageLimit = properties.getPageLimit();
			final int totalCount = userinfoDao.getSize().intValue();
			int maxPage = (totalCount/pageLimit)+(totalCount%pageLimit>0?1:0);
            int currentPage = 0; //当前页从0开始计算

            if (maxPage > properties.getPageMax()) {
			    maxPage = properties.getPageMax();
            }
			try {
				final String requestPage = request.getParameter("p");
				if (requestPage != null) {
					currentPage = Integer.parseInt(requestPage);
				}
				//越界检查，这里是做容错，可选调到其他界面
				if (currentPage >= maxPage) {
				    currentPage = maxPage-1;
                }
                if (currentPage < 0) {
				    currentPage = 0;
                }
			} catch (Exception e) {
				e.printStackTrace();
			}

            Collection<Userinfo> users = userinfoDao.getUsers(currentPage*pageLimit, pageLimit);
            List<HashMap <String, Object>> userList = new ArrayList<>();

            //当前用户信息
            Userinfo userinfo = userinfoDao.findUser(oldUserName);

            ModelAndView view = new ModelAndView("setting");

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
				}
			}

			if (userinfo != null) {
				view.addObject("username", oldUserName);
				view.addObject("role", userinfo.getRole()==LoginManager.LoginRole.Admin?"管理员":"普通用户");
			}
			view.addObject("usersrcs", userList);
			view.addObject("Path", request.getSession().getServletContext().getContextPath());
			view.addObject("maxPage", maxPage);
			view.addObject("currentPage", currentPage);
			List<Long> pageIndexes = new LinkedList<>();
			if (maxPage > 1) {
                if (maxPage < 7) {
                    for (long i = 0; i < maxPage; i++) {
                        pageIndexes.add(i);
                    }
                } else {
                    //前三页、后三页，其他情况
                    if (currentPage < 3) {
                        for (long i = 0; i < 7; i++) {
                            pageIndexes.add(i);
                        }
                    } else if (currentPage >= maxPage - 4) {
                        for (long i = maxPage - 7; i < maxPage; i++) {
                            pageIndexes.add(i);
                        }
                    } else {
                        for (long i = currentPage - 3; i < currentPage + 4; i++) {
                            pageIndexes.add(i);
                        }
                    }
                }
                view.addObject("pageIndexes", pageIndexes);
            }
			return view;
		}
		return LoginManager.redirectToRoot(session, response);
	}
}
