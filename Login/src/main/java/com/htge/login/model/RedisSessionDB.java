package com.htge.login.model;

import com.htge.login.util.LoginManager;
import org.apache.shiro.session.Session;
import org.jboss.logging.Logger;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.*;

public class RedisSessionDB implements SessionDBImpl {
    private Logger logger = Logger.getLogger(RedisSessionDB.class);
    private RedisTemplate<String, Object> template = null;
    private RedisTemplate<String, Object> userTemplate = null;

    public void setFactoryManager(RedisFactoryManager factoryManager) {
        template = factoryManager.getTemplate(1);
        userTemplate = factoryManager.getTemplate(2);
    }

    /* CRUD */
    public Session get(Serializable key) {
        Object value = template.opsForValue().get(key);
        if (value instanceof Session) {
            return (Session) value;
        }
        return null;
    }

    public void update(Session session) {
        template.opsForValue().set(session.getId().toString(), session);
        String user = (String)session.getAttribute(LoginManager.SESSION_USER_KEY);
        if (user != null) { //更新用户与session的关联关系
            updateUser(session, user);
        }
    }

    public void delete(Session session) {
        template.delete(session.getId().toString());
    }

    /* 如果已经在其他地方登录，这里做剔出逻辑 */
    private void updateUser(Session newSession, String user) {
        Serializable oldId = (Serializable) userTemplate.opsForValue().get(user);
        if (oldId != null && !oldId.equals(newSession.getId())) {
            Session session = get(oldId);
            //从缓存查到以后，旧的Session立即失效
            if (session != null && session.getAttribute(LoginManager.SESSION_USER_KEY) != null) {
                session.removeAttribute(LoginManager.SESSION_USER_KEY);
                session.setTimeout(0);
                update(session);
                logger.info("remove SessionID:" + newSession.getId().toString());
            }
        }
        logger.info("update SessionID:"+newSession.getId().toString());
        userTemplate.opsForValue().set(user, newSession.getId());
    }
}
