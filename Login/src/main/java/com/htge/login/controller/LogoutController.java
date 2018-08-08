package com.htge.login.controller;

import com.htge.login.util.LoginManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequestMapping("/auth")
public class LogoutController {
    private LoginManager loginManager;

    public void setLoginManager(LoginManager loginManager) {
        this.loginManager = loginManager;
    }

    @RequestMapping(value="/logout", method= RequestMethod.GET)
    public ModelAndView logoutPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        loginManager.Logout();
        loginManager.redirectToLogin(request, response);
        return null;
    }
}
