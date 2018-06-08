package com.htge.login.controller;

import com.htge.login.util.LoginManager;
import org.apache.shiro.SecurityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/auth")
public class LogoutController {
    @RequestMapping(value="/logout", method= RequestMethod.GET)
    public ModelAndView logoutPage() {
        LoginManager.Logout(SecurityUtils.getSubject());
        return new ModelAndView("redirect:/auth/");
    }
}
