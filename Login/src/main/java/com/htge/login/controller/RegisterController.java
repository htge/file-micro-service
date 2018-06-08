package com.htge.login.controller;

import com.htge.login.controller.Map.RegisterMap;
import com.htge.login.model.UserData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/auth")
public class RegisterController {
    @Autowired
    RegisterMap registerMap;

    @RequestMapping(value="/register", method= RequestMethod.GET)
    public Object registerPage(HttpServletRequest request) {
        return registerMap.registerPage(request);
    }

    @RequestMapping(value="/register", method=RequestMethod.POST)
    public ResponseEntity register(@ModelAttribute UserData userData) {
        return registerMap.register(userData);
    }
}
