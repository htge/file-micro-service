package com.htge.login.rabbit;

import com.htge.login.config.LoginProperties;
import com.htge.login.model.RedisSessionDao;
import com.htge.login.model.UserinfoDao;
import com.htge.login.model.Userinfo;
import com.htge.login.util.LoginManager;
import net.sf.json.JSONObject;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SimpleSession;
import org.jboss.logging.Logger;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

public class AuthRPCData implements RPCData {
    private final Logger logger = Logger.getLogger(AuthRPCData.class);

    private LoginProperties properties;
    private RedisSessionDao sessionDao;
    private UserinfoDao userinfoDao;

    public void setProperties(LoginProperties properties) {
        this.properties = properties;
    }

    public void setSessionDao(RedisSessionDao sessionDao) {
        this.sessionDao = sessionDao;
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
        }
        return retStr;
    }

    private JSONObject getLoginInfo(@NotNull JSONObject jsonObject) {
        JSONObject ret = new JSONObject();
        Serializable sessionId = null;
        if (jsonObject.has("sessionId")) {
            sessionId = jsonObject.getString("sessionId");
        }
        String username = null;
        int role = 0;
        Boolean isValidSession;
        try {
            if (sessionId != null) {
                SimpleSession session;
                try {
                    session = (SimpleSession) sessionDao.readSession(sessionId);
                } catch (UnknownSessionException e) {
                    session = new SimpleSession();
                    session.setId(sessionId);
                }
                if (session != null) {
                    //验证后，设置访问时间自动续期
                    session.validate();
                    session.setLastAccessTime(new Date());
                    sessionDao.update(session);

                    username = (String)session.getAttribute(LoginManager.SESSION_USER_KEY);
                    if (username != null) {
                        Userinfo userinfo = userinfoDao.findUser(username);
                        if (userinfo != null) {
                            role = userinfo.getRole();
                        }
                    } else if (jsonObject.has("redirectPath")) {
                        //未登录，记录跳转路径
                        String redirectPath = jsonObject.getString("redirectPath");
                        session.setAttribute(LoginManager.URL_KEY, redirectPath);
                    }
                }
            }
            isValidSession = (username != null);
        } catch (Exception e) {
            //验证超时或者其他情况
            isValidSession = false;
        }

        ret.put("role", role);
        ret.put("isValidSession", isValidSession);
        //通过域名访问的话，要把具体域名和端口拿出来
        ret.put("rootPath", properties.getRootPath());
        ret.put("settingPath", properties.getRootPath()+"setting");
        return ret;
    }
}
