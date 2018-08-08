package com.htge.login.controller;

import com.htge.login.model.Userinfo;
import com.htge.login.model.UserinfoDao;
import com.htge.login.util.LoginManager;
import net.sf.json.JSONObject;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;

@RequestMapping("/auth")
public class UserListController {
    private UserinfoDao userinfoDao;

    public void setUserinfoDao(UserinfoDao userinfoDao) {
        this.userinfoDao = userinfoDao;
    }

    @RequestMapping(value = "/userList", method = RequestMethod.GET)
    @ResponseBody
    public JSONObject userList(HttpServletRequest request) {
        int draw = Integer.parseInt(request.getParameter("draw"));
        int start = Integer.parseInt(request.getParameter("start"));
        int length = Integer.parseInt(request.getParameter("length"));
        JSONObject jsonObject = new JSONObject();

        try {
            Session session = SecurityUtils.getSubject().getSession();
            String oldUserName = (String)session.getAttribute(LoginManager.SESSION_USER_KEY);
            Collection<Userinfo> users = userinfoDao.getUsersExceptUser(oldUserName, start, length);
            ArrayList<ArrayList<String>> data = new ArrayList<>();

            for (Userinfo userinfo : users) {
                ArrayList<String> column = new ArrayList<>();
                column.add(userinfo.getUsername());
                column.add(userinfo.getRole() == LoginManager.LoginRole.Admin ? "管理员" : "普通用户");
                data.add(column);
            }

            //扣除当前账户
            final int totalCount = userinfoDao.getSize().intValue()-1;

            jsonObject.put("draw", draw);
            jsonObject.put("recordsTotal", totalCount);
            jsonObject.put("recordsFiltered", totalCount);
            jsonObject.put("data", data);
        } catch (Exception e) {
            e.printStackTrace();
            jsonObject.put("error", e.getMessage());
        }
        return jsonObject;
    }
}
