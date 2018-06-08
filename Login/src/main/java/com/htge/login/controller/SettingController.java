package com.htge.login.controller;

import com.htge.login.controller.Map.SettingMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/auth")
public class SettingController {
    @Autowired
    SettingMap settingMap;

    @RequestMapping(value="/setting", method= RequestMethod.GET)
    public ModelAndView setting(HttpServletRequest request) {
        return settingMap.settingPage(request);
    }
}
