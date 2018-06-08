package com.htge.login.rabbit;

import com.htge.login.config.LoginProperties;
import com.htge.login.model.SessionDBImpl;
import com.htge.login.model.UserinfoDao;
import com.htge.login.model.Userinfo;
import com.htge.login.util.LoginManager;
import net.sf.json.JSONObject;
import org.apache.shiro.session.Session;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Component
public class AuthRPCData implements RPCData {
    private SessionDBImpl sessionDB;
    private UserinfoDao userinfoDao;
    private final Logger logger = Logger.getLogger(AuthRPCData.class);

    @Autowired
    LoginProperties config;

    public void setSessionDB(SessionDBImpl sessionDB) {
        this.sessionDB = sessionDB;
    }

    public void setUserinfoDao(UserinfoDao userinfoDao) {
        this.userinfoDao = userinfoDao;
    }

    public String parseData(String data) {
        logger.info("RPC Request: "+data);
        JSONObject ret = new JSONObject();
        String retStr;
        try {
            JSONObject jsonObject = JSONObject.fromObject(data);
            if (jsonObject != null) {
                ret = getLoginInfo(jsonObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ret.put("error", e.getLocalizedMessage());
        } finally {
            retStr = ret.toString();
            logger.info("RPC Response: "+retStr);
        }
        return retStr;
    }

    private JSONObject getLoginInfo(@NotNull JSONObject jsonObject) {
        JSONObject ret = new JSONObject();
        if (!jsonObject.has("sessionId")) {
            ret.put("error", "sessionId not found");
            return ret;
        }
        Serializable sessionId = jsonObject.getString("sessionId");
        Session session = sessionDB.get(sessionId);
        String username = null;
        int role = 0;
        if (session != null) {
            username = (String)session.getAttribute(LoginManager.SESSION_USER_KEY);
            if (username != null) {
                Userinfo userinfo = userinfoDao.findUser(username);
                if (userinfo != null) {
                    role = userinfo.getRole();
                }
            }
        }
        ret.put("role", role);
        Boolean isValidSession = (username != null);
        ret.put("isValidSession", isValidSession);
        //通过域名访问的话，要把具体域名和端口拿出来
        ret.put("rootPath", config.getRootPath());
        ret.put("logoutPath", config.getRootPath()+"logout");
        //管理员，添加设置路径
        if (role == LoginManager.LoginRole.Admin) {
            ret.put("settingPath", config.getRootPath()+"setting");
        }
        return ret;
    }
}
