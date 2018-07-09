package com.htge.login.controller;

import com.htge.login.controller.Map.DeleteMap;
import com.htge.login.model.UserData;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/auth")
public class DeleteController {
    @Resource(name = "deleteMap")
    private DeleteMap deleteMap;

    @RequestMapping(value="/delete/{username}", method= RequestMethod.GET)
    public Object setting(@PathVariable String username, HttpServletRequest request, HttpServletResponse response) {
        return deleteMap.deletePage(username, request, response);
    }

    @RequestMapping(value="/delete", method=RequestMethod.POST)
    public ResponseEntity setting(@ModelAttribute UserData userData) {
        return deleteMap.deleteUser(userData);
    }
}
