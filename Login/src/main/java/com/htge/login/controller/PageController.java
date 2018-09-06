package com.htge.login.controller;

import com.htge.login.config.LoginProperties;
import com.htge.login.model.Userinfo;
import com.htge.login.model.UserinfoDao;
import com.htge.login.util.Crypto;
import com.htge.login.util.LoginManager;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyPair;

@RequestMapping("/auth")
public class PageController {
    private LoginManager loginManager;
    private UserinfoDao userinfoDao;
    private LoginProperties properties;

    public void setLoginManager(LoginManager loginManager) {
        this.loginManager = loginManager;
    }
    public void setUserinfoDao(UserinfoDao userinfoDao) {
        this.userinfoDao = userinfoDao;
    }
    public void setProperties(LoginProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/")
    public Object loginPage() throws Exception {
        KeyPair keyPair = loginManager.generateKeyPair();
        String rsaPublicKey = Crypto.getPublicKey(keyPair);
        String uuid = loginManager.generateUUID();

        ModelAndView view = new ModelAndView("login");
        view.addObject("rsa", rsaPublicKey);
        view.addObject("uuid", uuid);
        return view;
    }

    @GetMapping("/change")
    public Object changePage() throws Exception {
        KeyPair keyPair = loginManager.generateKeyPair();
        String rsaPublicKey = Crypto.getPublicKey(keyPair);
        String uuid = loginManager.generateUUID();

        ModelAndView view = new ModelAndView("change");
        view.addObject("rsa", rsaPublicKey);
        view.addObject("uuid", uuid);
        return view;
    }

    @GetMapping("/delete/{username}")
    public Object deletePage(@PathVariable String username) throws Exception {
        KeyPair keyPair = loginManager.generateKeyPair();
        String rsaPublicKey = Crypto.getPublicKey(keyPair);
        String uuid = loginManager.generateUUID();

        ModelAndView view = new ModelAndView("delete");
        view.addObject("username", username);
        view.addObject("rsa", rsaPublicKey);
        view.addObject("uuid", uuid);
        return view;
    }

    @GetMapping("/register")
    public Object registerPage() throws Exception {
        KeyPair keyPair = loginManager.generateKeyPair();
        String rsaPublicKey = Crypto.getPublicKey(keyPair);
        String uuid = loginManager.generateUUID();

        ModelAndView view = new ModelAndView("register");
        view.addObject("rsa", rsaPublicKey);
        view.addObject("uuid", uuid);
        return view;
    }

    @GetMapping("/setting")
    public ModelAndView settingPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Session session = SecurityUtils.getSubject().getSession();
        String oldUserName = (String)session.getAttribute(LoginManager.SESSION.USER_KEY);

        //当前用户信息
        Userinfo userinfo = userinfoDao.findUser(oldUserName);
        if (userinfo != null) {
            ModelAndView view = new ModelAndView("setting");
            view.addObject("username", oldUserName);
            view.addObject("role", userinfo.getRole()==LoginManager.LoginRole.Admin?"管理员":"普通用户");
            view.addObject("uploadPath", properties.getUploadRoot());
            return view;
        }
        loginManager.redirectToLogin(request, response);
        return null;
    }

    @GetMapping("/logout")
    public ModelAndView logoutPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        loginManager.Logout();
        loginManager.redirectToLogin(request, response);
        return null;
    }
}
