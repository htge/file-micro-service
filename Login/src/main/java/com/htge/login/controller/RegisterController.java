package com.htge.login.controller;

import com.htge.login.controller.Map.RegisterMap;
import com.htge.login.model.UserData;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/auth")
public class RegisterController {
    @Resource(name = "registerMap")
    private RegisterMap registerMap;

    @RequestMapping(value="/register", method= RequestMethod.GET)
    public Object registerPage(HttpServletRequest request, HttpServletResponse response) {
        return registerMap.registerPage(request, response);
    }

    @RequestMapping(value="/register", method=RequestMethod.POST)
    public ResponseEntity register(@ModelAttribute UserData userData) {
        return registerMap.register(userData);
    }
}
