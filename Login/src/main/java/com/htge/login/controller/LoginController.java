package com.htge.login.controller;

import com.htge.login.controller.Map.LoginMap;
import com.htge.login.model.UserData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Controller
@RequestMapping("/auth")
public class LoginController {
    @Autowired
    LoginMap loginMap;

    @RequestMapping(value="/", method = RequestMethod.GET)
    public Object loginPage(HttpServletRequest request) {
        return loginMap.loginPage(request);
    }

    @ResponseBody
    @RequestMapping(value="/login", method = RequestMethod.POST)
    public Object login(@ModelAttribute UserData userData) throws IOException {
        return loginMap.login(userData);
    }
}
