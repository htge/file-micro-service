package com.htge.login.controller;

import com.htge.login.controller.Map.LoginMap;
import com.htge.login.model.UserData;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("/auth")
public class LoginController {
    @Resource(name = "loginMap")
    private LoginMap loginMap;

    @RequestMapping(value="/", method = RequestMethod.GET)
    public Object loginPage(HttpServletRequest request, HttpServletResponse response) {
        return loginMap.loginPage(request, response);
    }

    @ResponseBody
    @RequestMapping(value="/login", method = RequestMethod.POST)
    public Object login(HttpServletRequest request, @ModelAttribute UserData userData) throws IOException {
        return loginMap.login(request, userData);
    }
}
