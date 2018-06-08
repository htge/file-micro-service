package com.htge.login.controller;

import com.htge.login.controller.Map.ChangeMap;
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
public class ChangeController {

    @Autowired
    ChangeMap changeMap;

    @RequestMapping(value="/change", method= RequestMethod.GET)
    public Object setting(HttpServletRequest request) {
        return changeMap.changePage(request);
    }

    @RequestMapping(value="/change", method=RequestMethod.POST)
    public ResponseEntity setting(@ModelAttribute UserData userData) {
        return changeMap.changeUserInfo(userData);
    }
}
