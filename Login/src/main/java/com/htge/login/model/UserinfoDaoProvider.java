package com.htge.login.model;

import java.util.List;
import java.util.Map;

public class UserinfoDaoProvider {
    public String insertList(Map map) {
        List userinfos = (List)map.get("list");
        StringBuilder sb = new StringBuilder();

        sb.append("INSERT INTO USERINFO ");
        sb.append("(username, userdata, role) ");
        sb.append("VALUES ");
        for (int i=0; i<userinfos.size(); i++) {
            sb.append(String.format("(#{list[%d].username}, #{list[%d].userdata}, #{list[%d].role})", i, i, i));
            if (i < userinfos.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
