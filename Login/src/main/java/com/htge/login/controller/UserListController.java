package com.htge.login.controller;

import com.htge.login.controller.Map.UserListMap;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/auth")
public class UserListController {
    @Resource(name = "userListMap")
    private UserListMap userListMap;

    @RequestMapping(value = "/userList", method = RequestMethod.GET)
    @ResponseBody
    public JSONObject userList(HttpServletRequest request) {
        return userListMap.userList(request);
    }
}
