package com.htge.login.controller;

import com.htge.login.controller.Map.SettingMap;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/auth")
public class SettingController {
    @Resource(name = "settingMap")
    private SettingMap settingMap;

    @RequestMapping(value="/setting", method= RequestMethod.GET)
    public ModelAndView setting(HttpServletRequest request, HttpServletResponse response) {
        return settingMap.settingPage(request, response);
    }
}
