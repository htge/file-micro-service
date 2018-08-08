package com.htge.login.controller;

import com.htge.login.config.LoginProperties;
import com.htge.login.model.Userinfo;
import com.htge.login.model.UserinfoDao;
import com.htge.login.util.LoginManager;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@RequestMapping("/auth")
public class SettingController {
    private UserinfoDao userinfoDao;
    private LoginProperties properties;

    public void setUserinfoDao(UserinfoDao userinfoDao) {
        this.userinfoDao = userinfoDao;
    }
    public void setProperties(LoginProperties properties) {
        this.properties = properties;
    }

    @RequestMapping(value="/setting", method= RequestMethod.GET)
    public ModelAndView setting() {
        Session session = SecurityUtils.getSubject().getSession();
        String oldUserName = (String)session.getAttribute(LoginManager.SESSION_USER_KEY);

        //当前用户信息
        Userinfo userinfo = userinfoDao.findUser(oldUserName);
        ModelAndView view = new ModelAndView("setting");

        if (userinfo != null) {
            view.addObject("username", oldUserName);
            view.addObject("role", userinfo.getRole()==LoginManager.LoginRole.Admin?"管理员":"普通用户");
            view.addObject("uploadPath", properties.getUploadRoot());
        }
        return view;
    }
}
