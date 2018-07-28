package com.htge.login.controller.Map;

import com.htge.login.config.LoginProperties;
import com.htge.login.model.Userinfo;
import com.htge.login.model.UserinfoDao;
import com.htge.login.util.LoginManager;
import net.sf.json.JSONObject;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class UserListMap {
    private UserinfoDao userinfoDao;
    private LoginProperties properties;

    public void setUserinfoDao(UserinfoDao userinfoDao) {
        this.userinfoDao = userinfoDao;
    }

    public void setProperties(LoginProperties properties) {
        this.properties = properties;
    }

    public JSONObject userList(HttpServletRequest request) {
        Subject subject = SecurityUtils.getSubject();
        if (!LoginManager.isAuthorized(subject)) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", "user not authorized!");
            return jsonObject;
        }

        int draw = Integer.parseInt(request.getParameter("draw"));
        int start = Integer.parseInt(request.getParameter("start"));
        int length = Integer.parseInt(request.getParameter("length"));
        JSONObject jsonObject = new JSONObject();
        Session session = subject.getSession();

        try {
            String oldUserName = (String)session.getAttribute(LoginManager.SESSION_USER_KEY);
            Userinfo oldUserInfo = userinfoDao.findUser(oldUserName);
            ArrayList<ArrayList<String>> data = new ArrayList<>();

            if (oldUserInfo.getRole() == LoginManager.LoginRole.Admin) {
                Collection<Userinfo> users = userinfoDao.getUsersExceptUser(oldUserName, start, length);

                for (Userinfo userinfo : users) {
                    ArrayList<String> column = new ArrayList<>();
                    column.add(userinfo.getUsername());
                    column.add(userinfo.getRole() == LoginManager.LoginRole.Admin ? "管理员" : "普通用户");
                    data.add(column);
                }
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


