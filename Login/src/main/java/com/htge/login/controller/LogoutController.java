package com.htge.login.controller;

import com.htge.login.util.LoginManager;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/auth")
public class LogoutController {
    @RequestMapping(value="/logout", method= RequestMethod.GET)
    public ModelAndView logoutPage(HttpServletRequest request, HttpServletResponse response) {
        LoginManager.Logout(SecurityUtils.getSubject());
        Subject subject = SecurityUtils.getSubject();
        Session session = subject.getSession();
        return LoginManager.redirectToRoot(session, request, response);
    }
}
